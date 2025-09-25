package com.lonyitrade.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.lonyitrade.app.adapters.MessageAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.Message
import com.lonyitrade.app.data.models.UserProfile
import com.lonyitrade.app.utils.SessionManager
import com.lonyitrade.app.utils.WebSocketManager
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var ad: Ad
    private lateinit var seller: UserProfile
    private lateinit var myUserId: String
    private var messageList: MutableList<Message> = mutableListOf()
    private lateinit var messageAdapter: MessageAdapter
    private var selectedMediaUri: Uri? = null

    // UI Elements
    private lateinit var adTitleTextView: TextView
    private lateinit var adPriceTextView: TextView
    private lateinit var adPhotoImageView: ImageView
    private lateinit var chatToolbarTitle: TextView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var attachMediaButton: ImageView
    private lateinit var typingIndicatorTextView: TextView
    private lateinit var otherUserPhotoInToolbar: CircleImageView
    private lateinit var mediaPreviewLayout: FrameLayout
    private lateinit var previewImageView: ImageView
    private lateinit var closePreviewButton: ImageView
    private lateinit var captionEditText: EditText
    private lateinit var sendMediaButton: Button
    private lateinit var mainChatLayout: LinearLayout

    private var isTyping = false
    private var typingJob: kotlinx.coroutines.Job? = null
    private var typingAnimation: ObjectAnimator? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            showMediaPreview(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        sessionManager = SessionManager(this)
        ad = intent.getParcelableExtra("AD_EXTRA") ?: run {
            Toast.makeText(this, "Error loading ad data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (ad.userId == null) {
            Toast.makeText(this, "Seller details are missing. Cannot start chat.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        myUserId = sessionManager.getUserId() ?: ""

        initializeViews()
        setupListeners()
        setupRecyclerView()
        fetchSellerDetails()
        observeNewMessages()
        observeTypingNotifications()
        observeMessageStatusUpdates()
    }

    override fun onStop() {
        super.onStop()
        if (isTyping) {
            ad.id?.let { advertId ->
                ad.userId?.let { receiverId ->
                    WebSocketManager.sendStopTypingEvent(advertId, receiverId)
                }
            }
            isTyping = false
        }
    }

    private fun initializeViews() {
        chatToolbarTitle = findViewById(R.id.chatToolbarTitle)
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
        adTitleTextView = findViewById(R.id.adTitleTextView)
        adPriceTextView = findViewById(R.id.adPriceTextView)
        adPhotoImageView = findViewById(R.id.adPhotoImageView)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        attachMediaButton = findViewById(R.id.attachMediaButton)
        typingIndicatorTextView = findViewById(R.id.typingIndicatorTextView)
        otherUserPhotoInToolbar = findViewById(R.id.otherUserPhotoInToolbar)

        // Media Preview UI
        mediaPreviewLayout = findViewById(R.id.mediaPreviewLayout)
        previewImageView = findViewById(R.id.previewImageView)
        closePreviewButton = findViewById(R.id.closePreviewButton)
        captionEditText = findViewById(R.id.captionEditText)
        sendMediaButton = findViewById(R.id.sendMediaButton)
        mainChatLayout = findViewById(R.id.mainChatLayout)
    }

    private fun setupListeners() {
        sendButton.setOnClickListener {
            val content = messageEditText.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content, ad.userId!!)
                if (ad.id != null && ad.userId != null) {
                    WebSocketManager.sendStopTypingEvent(ad.id!!, ad.userId!!)
                }
                isTyping = false
            }
        }
        attachMediaButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (ad.id == null || ad.userId == null) return
                if (!isTyping) {
                    isTyping = true
                    WebSocketManager.sendTypingEvent(ad.id!!, ad.userId!!)
                }
                typingJob?.cancel()
                typingJob = lifecycleScope.launch(Dispatchers.Main) {
                    delay(3000)
                    isTyping = false
                    WebSocketManager.sendStopTypingEvent(ad.id!!, ad.userId!!)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Media Preview Listeners
        closePreviewButton.setOnClickListener {
            hideMediaPreview()
        }
        sendMediaButton.setOnClickListener {
            selectedMediaUri?.let { uri ->
                val caption = captionEditText.text.toString().trim()
                uploadMedia(uri, caption)
                hideMediaPreview()
            }
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList, myUserId)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messagesRecyclerView.adapter = messageAdapter
    }

    private fun showMediaPreview(uri: Uri) {
        mainChatLayout.visibility = View.GONE
        mediaPreviewLayout.visibility = View.VISIBLE
        Glide.with(this).load(uri).into(previewImageView)
    }

    private fun hideMediaPreview() {
        mediaPreviewLayout.visibility = View.GONE
        mainChatLayout.visibility = View.VISIBLE
        captionEditText.text.clear()
        selectedMediaUri = null
    }

    private fun fetchSellerDetails() {
        val sellerUserId = ad.userId ?: return
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getUserById("Bearer $token", sellerUserId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        seller = response.body()!!
                        populateAdAndChatHeader()
                        fetchMessages()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error fetching seller details: ${e.message}")
            }
        }
    }

    private fun observeNewMessages() {
        WebSocketManager.newMessage.observe(this) { message ->
            if (message.advertId == ad.id && (message.senderId == myUserId || message.receiverId == myUserId)) {
                // If this is a confirmation of a message we just sent, replace the temporary one
                val existingIndex = messageList.indexOfFirst { it.id == "temporaryId" && it.content == message.content }
                if (existingIndex != -1) {
                    messageList[existingIndex] = message
                    messageAdapter.notifyItemChanged(existingIndex)
                } else {
                    // It's a new message from the other user
                    messageList.add(message)
                    messageAdapter.notifyItemInserted(messageList.size - 1)
                }
                messagesRecyclerView.scrollToPosition(messageList.size - 1)
                // If the message is from the other user and its status is not 'read', mark it as read
                if (message.senderId == ad.userId && message.status != "read") {
                    WebSocketManager.markMessagesAsRead(ad.id!!, ad.userId!!)
                }
            }
        }
    }

    private fun observeMessageStatusUpdates() {
        WebSocketManager.messageStatusUpdate.observe(this) { (messageIds, status) ->
            messageIds.forEach { messageId ->
                val index = messageList.indexOfFirst { it.id == messageId }
                if (index != -1) {
                    messageList[index] = messageList[index].copy(status = status)
                    messageAdapter.notifyItemChanged(index)
                }
            }
        }
    }

    private fun sendMessage(content: String, receiverId: String) {
        val advertId = ad.id ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(Date())

        // Create a temporary message to display instantly
        val tempMessage = Message(
            id = "temporaryId",
            senderId = myUserId,
            receiverId = receiverId,
            advertId = advertId,
            content = content,
            createdAt = timestamp,
            mediaUrl = null,
            status = "sent" // Initial status
        )
        messageList.add(tempMessage)
        messageAdapter.notifyItemInserted(messageList.size - 1)
        messagesRecyclerView.scrollToPosition(messageList.size - 1)
        messageEditText.text.clear()

        // Send the message via WebSocket
        val payload = mapOf(
            "receiver_id" to receiverId,
            "advert_id" to advertId,
            "content" to content
        )
        val messagePayload = mapOf("type" to "newMessage", "payload" to payload)
        val jsonMessage = Gson().toJson(messagePayload)
        WebSocketManager.sendMessage(jsonMessage)
    }


    private fun populateAdAndChatHeader() {
        chatToolbarTitle.text = seller.fullName
        seller.profilePictureUrl?.let { url ->
            val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + url.trimStart('/')
            Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_profile_placeholder).into(otherUserPhotoInToolbar)
        }
        adTitleTextView.text = ad.title
        adPriceTextView.text = "UGX ${ad.price}"
        val firstPhotoUrl = ad.photos?.firstOrNull()
        if (firstPhotoUrl != null) {
            val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + firstPhotoUrl.trimStart('/')
            Glide.with(this).load(imageUrl).into(adPhotoImageView)
        } else {
            adPhotoImageView.setImageResource(R.drawable.ic_add_photo)
        }
    }

    private fun fetchMessages() {
        val sellerUserId = ad.userId ?: return
        val advertId = ad.id ?: return
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getMessages("Bearer $token", advertId, sellerUserId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val messages = response.body() ?: emptyList()
                        messageList.clear()
                        messageList.addAll(messages)
                        messageAdapter.notifyDataSetChanged()
                        messagesRecyclerView.scrollToPosition(messageList.size - 1)
                        if (messages.any { it.receiverId == myUserId && it.status != "read" }) {
                            WebSocketManager.markMessagesAsRead(advertId, sellerUserId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error fetching messages: ${e.message}")
            }
        }
    }

    private fun uploadMedia(uri: Uri, caption: String?) {
        val token = sessionManager.fetchAuthToken() ?: return
        val advertId = ad.id ?: return
        val receiverId = ad.userId ?: return

        getTempFileFromUri(uri)?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val mediaPart = MultipartBody.Part.createFormData("media", file.name, requestFile)
            val advertIdRequestBody = advertId.toRequestBody("text/plain".toMediaTypeOrNull())
            val receiverIdRequestBody = receiverId.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionRequestBody = caption?.ifEmpty { null }?.toRequestBody("text/plain".toMediaTypeOrNull())

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = ApiClient.apiService.uploadMessageMedia("Bearer $token", advertIdRequestBody, receiverIdRequestBody, mediaPart, captionRequestBody)
                    withContext(Dispatchers.Main) {
                        if (!response.isSuccessful) {
                            Toast.makeText(this@ChatActivity, "Failed to upload media: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChatActivity, "Error uploading media: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getTempFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "temp_media_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun observeTypingNotifications() {
        WebSocketManager.typingNotification.observe(this) { (senderId, advertId, isTyping) ->
            if (senderId == ad.userId && advertId == ad.id) {
                if (isTyping) {
                    typingIndicatorTextView.visibility = View.VISIBLE
                    startTypingAnimation()
                } else {
                    typingIndicatorTextView.visibility = View.GONE
                    stopTypingAnimation()
                }
            }
        }
    }

    private fun startTypingAnimation() {
        if (typingAnimation == null) {
            typingAnimation = ObjectAnimator.ofFloat(typingIndicatorTextView, "alpha", 0.5f, 1f).apply {
                duration = 500
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
            }
            typingAnimation?.start()
        }
    }

    private fun stopTypingAnimation() {
        typingAnimation?.cancel()
        typingAnimation = null
        typingIndicatorTextView.alpha = 1f
    }
}
