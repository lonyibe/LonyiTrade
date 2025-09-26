package com.lonyitrade.app

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import java.io.IOException
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
    private lateinit var micButton: ImageView
    private lateinit var recordingTimerTextView: TextView

    private var isTyping = false
    private var typingJob: kotlinx.coroutines.Job? = null
    private var typingAnimation: ObjectAnimator? = null

    // Audio Recording
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())
    private var recordTime: Long = 0

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

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
        checkAndRequestPermissions()
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
        if (isRecording) {
            stopRecording(send = false) // Cancel recording if activity is stopped
        }
        // Release MediaPlayer from adapter
        if (::messageAdapter.isInitialized) {
            messageAdapter.releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseRecorder()
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
        micButton = findViewById(R.id.micButton)
        recordingTimerTextView = findViewById(R.id.recordingTimerTextView)


        // Media Preview UI
        mediaPreviewLayout = findViewById(R.id.mediaPreviewLayout)
        previewImageView = findViewById(R.id.previewImageView)
        closePreviewButton = findViewById(R.id.closePreviewButton)
        captionEditText = findViewById(R.id.captionEditText)
        sendMediaButton = findViewById(R.id.sendMediaButton)
        mainChatLayout = findViewById(R.id.mainChatLayout)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        sendButton.setOnClickListener {
            val content = messageEditText.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content, null, ad.userId!!)
                if (ad.id != null && ad.userId != null) {
                    WebSocketManager.sendStopTypingEvent(ad.id!!, ad.userId!!)
                }
                isTyping = false
            }
        }
        attachMediaButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        micButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkAndRequestPermissions()) {
                        startRecording()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopRecording(send = true)
                    true
                }
                else -> false
            }
        }

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    sendButton.visibility = View.GONE
                    micButton.visibility = View.VISIBLE
                    attachMediaButton.visibility = View.VISIBLE
                } else {
                    sendButton.visibility = View.VISIBLE
                    micButton.visibility = View.GONE
                    attachMediaButton.visibility = View.GONE
                }

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
                val existingIndex = messageList.indexOfFirst { it.id == "temporaryId" && (it.content == message.content || it.audioUrl != null) }
                if (existingIndex != -1) {
                    messageList[existingIndex] = message
                    messageAdapter.notifyItemChanged(existingIndex)
                } else {
                    messageList.add(message)
                    messageAdapter.notifyItemInserted(messageList.size - 1)
                }
                messagesRecyclerView.scrollToPosition(messageList.size - 1)
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

    private fun sendMessage(content: String?, audioUrl: String?, receiverId: String) {
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
            audioUrl = audioUrl,
            status = "sent"
        )
        messageList.add(tempMessage)
        messageAdapter.notifyItemInserted(messageList.size - 1)
        messagesRecyclerView.scrollToPosition(messageList.size - 1)
        messageEditText.text.clear()

        // Send the message via WebSocket
        val payload = mutableMapOf(
            "receiver_id" to receiverId,
            "advert_id" to advertId,
        )
        content?.let { payload["content"] = it }
        audioUrl?.let { payload["audio_url"] = it }

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

    // --- Audio Recording Functions ---

    private fun checkAndRequestPermissions(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permission denied. Cannot record audio.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecording() {
        if (isRecording) return
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        audioFile = File(externalCacheDir, "AUDIO_$timestamp.m4a")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                recordingTimerTextView.visibility = View.VISIBLE
                messageEditText.visibility = View.GONE
                attachMediaButton.visibility = View.GONE
                startTimer()
            } catch (e: IOException) {
                Log.e("ChatActivity", "MediaRecorder prepare() failed", e)
                Toast.makeText(this@ChatActivity, "Recording failed to start", Toast.LENGTH_SHORT).show()
                releaseRecorder()
            }
        }
    }

    private fun stopRecording(send: Boolean) {
        if (!isRecording) return
        try {
            mediaRecorder?.stop()
            if (send) {
                audioFile?.let { uploadAudio(it) }
            } else {
                audioFile?.delete()
            }
        } catch (e: RuntimeException) {
            Log.w("ChatActivity", "MediaRecorder stop() failed", e)
            audioFile?.delete()
        } finally {
            releaseRecorder()
            isRecording = false
            recordingTimerTextView.visibility = View.GONE
            messageEditText.visibility = View.VISIBLE
            if (messageEditText.text.isEmpty()) {
                attachMediaButton.visibility = View.VISIBLE
            }
            stopTimer()
        }
    }

    private fun releaseRecorder() {
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun startTimer() {
        recordTime = 0
        handler.post(object : Runnable {
            override fun run() {
                if (isRecording) {
                    recordTime++
                    val minutes = (recordTime % 3600) / 60
                    val seconds = recordTime % 60
                    recordingTimerTextView.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun stopTimer() {
        handler.removeCallbacksAndMessages(null)
        recordingTimerTextView.text = "00:00"
    }

    private fun uploadAudio(file: File) {
        val token = sessionManager.fetchAuthToken() ?: return
        val advertId = ad.id ?: return
        val receiverId = ad.userId ?: return

        if (!file.exists() || file.length() == 0L) {
            Log.e("ChatActivity", "Audio file is empty or does not exist.")
            return
        }

        val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
        val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestFile)
        val advertIdRequestBody = advertId.toRequestBody("text/plain".toMediaTypeOrNull())
        val receiverIdRequestBody = receiverId.toRequestBody("text/plain".toMediaTypeOrNull())


        // Display a temporary message
        sendMessage(null, file.absolutePath, receiverId)


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // NOTE: We will create the `uploadMessageAudio` endpoint on the backend next.
                // This call will fail for now, but the WebSocket message will handle the real update.
                ApiClient.apiService.uploadMessageAudio("Bearer $token", advertIdRequestBody, receiverIdRequestBody, audioPart)
            } catch (e: Exception) {
                Log.e("ChatActivity", "Failed to upload audio via API. WebSocket will handle it.")
            } finally {
                // The websocket will send the real message with the remote URL
            }
        }
    }
}

