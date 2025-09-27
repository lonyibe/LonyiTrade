package com.lonyitrade.app

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
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
    private lateinit var chatPartner: UserProfile // Renamed from 'seller'
    private lateinit var myUserId: String
    private lateinit var chatPartnerId: String // New field to hold the ID of the person the user is chatting with
    private var messageList: MutableList<Message> = mutableListOf()
    private lateinit var messageAdapter: MessageAdapter
    private var selectedMediaUri: Uri? = null

    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(this) }

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
        const val PARTNER_ID_EXTRA = "PARTNER_ID_EXTRA" // Expected from MessagesFragment/AdDetailActivity
        private const val OTHER_USER_ID_NOTIFICATION = "otherUserId" // Expected from FCM data (but often missing)
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
        myUserId = sessionManager.getUserId() ?: "" // Initialize myUserId here

        initializeViews()
        setupListeners()
        setupRecyclerView()
        checkAndRequestPermissions()

        // Delegate the actual intent processing to a dedicated method
        handleIncomingIntent(intent)

        observeTypingNotifications()
        observeNewMessages()
        observeMessageStatusUpdates()
    }

    /**
     * @Override: Called when the activity is relaunched with a new Intent (e.g., from a notification)
     * The signature is corrected to take a non-nullable Intent.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * FIX: Refactored intent handling logic to correctly determine the chat partner and reload the chat if the context changes.
     * Includes a robust fallback mechanism for notifications missing the chat partner ID.
     */
    private fun handleIncomingIntent(intent: Intent) {
        val adParcelable = intent.getParcelableExtra("AD_EXTRA") as? Ad
        val adIdFromNotification = intent.getStringExtra("adId")
        val notificationPartnerId = intent.getStringExtra(OTHER_USER_ID_NOTIFICATION) // May be null

        val currentAdId = if (::ad.isInitialized) ad.id else null
        val currentChatPartnerId = if (::chatPartnerId.isInitialized) chatPartnerId else null

        // Case 1: Launched from MessagesFragment/AdDetailActivity (expected parcelable)
        if (adParcelable != null) {
            val incomingPartnerId = if (adParcelable.userId == myUserId) {
                // I am the seller (ad owner). The partner is the Buyer.
                // We MUST rely on the explicit ID passed by the ConversationsAdapter.
                intent.getStringExtra(PARTNER_ID_EXTRA)
            } else {
                // I am the buyer (not the ad owner). The partner is the Seller (Ad owner).
                adParcelable.userId
            } ?: run {
                // CRITICAL FIX: This path handles cases where the context is incomplete (e.g., launching from old notification without proper extras)
                Toast.makeText(this, "Error: Missing chat partner ID.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            if (currentChatPartnerId != incomingPartnerId || !::ad.isInitialized || ad.id != adParcelable.id) {
                ad = adParcelable
                chatPartnerId = incomingPartnerId
                fetchAdDetailsForAdExtra(ad) // Set ad, then fetch partner, then messages
            }
            return
        }

        // Case 2: Launched from Notification (deep link - expected adId)
        if (adIdFromNotification != null) {
            val isContextChange = currentAdId != adIdFromNotification || currentChatPartnerId != notificationPartnerId

            if (isContextChange) {
                if (notificationPartnerId != null) {
                    // Best case: Partner ID is in the notification data
                    chatPartnerId = notificationPartnerId
                    fetchAdDetailsForDeepLink(adIdFromNotification) // Now calls fetchChatPartnerDetails
                } else {
                    // FIX: Partner ID is missing. Crash prevention: fetch conversation list to find the partner ID.
                    fetchConversationAndAdDetails(adIdFromNotification)
                }
            }
            return
        }

        // Case 3: Error
        if (!::ad.isInitialized) {
            Toast.makeText(this, "Error: Missing conversation details.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /**
     * FIX: New method to handle logic when launching from AdExtra (ensures we get fresh partner details).
     */
    private fun fetchAdDetailsForAdExtra(adParcelable: Ad) {
        // We already have the AD object and the chatPartnerId is set.
        ad = adParcelable
        fetchChatPartnerDetails()
    }


    /**
     * FIX: New method to fetch partner ID from the conversation list when it's missing from the notification payload.
     */
    private fun fetchConversationAndAdDetails(adId: String) {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            Toast.makeText(this, "Please log in to view this conversation.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch all conversations to find the partner ID for this ad
                val conversationsResponse = apiService.getConversations("Bearer $token")
                if (!conversationsResponse.isSuccessful || conversationsResponse.body() == null) {
                    throw IOException("Failed to fetch conversation list.")
                }

                // Find the conversation summary matching the adId
                val conversation = conversationsResponse.body()?.firstOrNull { it.advertId == adId }
                val partnerId = conversation?.otherUserId //

                if (partnerId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChatActivity, "Error: Could not find chat partner for this ad.", Toast.LENGTH_LONG).show()
                        if (!::ad.isInitialized) finish()
                    }
                    return@launch // Use labeled return to exit only the coroutine block.
                }

                // 2. Set the partner ID and proceed to fetch the Ad details
                withContext(Dispatchers.Main) {
                    chatPartnerId = partnerId
                    fetchAdDetailsForDeepLink(adId) // This will now use the correct chatPartnerId
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error fetching conversation details: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error loading conversation details. Please try again.", Toast.LENGTH_LONG).show()
                    if (!::ad.isInitialized) finish()
                }
            }
        }
    }

    // Refactored fetchSellerDetails to fetchChatPartnerDetails.
    private fun fetchChatPartnerDetails() {
        val partnerId = chatPartnerId
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch the profile of the determined chat partner (buyer or seller)
                val response = apiService.getUserById("Bearer $token", partnerId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        chatPartner = response.body() ?: return@withContext
                        populateAdAndChatHeader()
                        fetchMessages()
                    } else {
                        Log.e("ChatActivity", "Failed to fetch chat partner details: ${response.code()}")
                        Toast.makeText(this@ChatActivity, "Failed to load chat partner info.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error fetching chat partner details: ${e.message}")
            }
        }
    }

    // Updated fetchAdDetailsForDeepLink to call fetchChatPartnerDetails.
    private fun fetchAdDetailsForDeepLink(adId: String) {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            Toast.makeText(this, "Please log in to view this conversation.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // CRITICAL FIX: The backend route /api/adverts/:id does not require the token.
                // FIX: Removed the token argument to match the ApiService function signature.
                val adResponse = apiService.getAdvertById(adId)

                if (!adResponse.isSuccessful || adResponse.body() == null) {
                    throw IOException("Failed to fetch Ad details for ID: $adId. HTTP Code: ${adResponse.code()}")
                }

                val fetchedAd = adResponse.body()!!

                // Set the fetched Ad and proceed
                withContext(Dispatchers.Main) {
                    ad = fetchedAd
                    // The chatPartnerId was set in handleIncomingIntent or fetchConversationAndAdDetails.
                    fetchChatPartnerDetails() // Will proceed with chat setup
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Deep link failed to fetch Ad: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error loading conversation details. Please try again.", Toast.LENGTH_LONG).show()
                    // Only finish if 'ad' wasn't initialized
                    if (!::ad.isInitialized) {
                        finish()
                    }
                }
            }
        }
    }
    // ---------------------------------------------------


    override fun onStop() {
        super.onStop()
        if (::ad.isInitialized) { // Check if 'ad' is initialized before using
            if (isTyping) {
                ad.id?.let { advertId ->
                    // FIX: Use chatPartnerId
                    if (::chatPartnerId.isInitialized) {
                        WebSocketManager.sendStopTypingEvent(advertId, chatPartnerId)
                    }
                }
                isTyping = false
            }
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
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
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
                if (!::chatPartnerId.isInitialized) return@setOnClickListener
                // FIX: Use chatPartnerId
                sendMessage(content, null, chatPartnerId)
                if (ad.id != null) {
                    WebSocketManager.sendStopTypingEvent(ad.id!!, chatPartnerId)
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
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    stopRecording(send = true)
                    true
                }
                else -> false
            }
        }

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence ?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence ?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    sendButton.visibility = View.GONE
                    micButton.visibility = View.VISIBLE
                    attachMediaButton.visibility = View.VISIBLE
                } else {
                    sendButton.visibility = View.VISIBLE
                    micButton.visibility = View.GONE
                    attachMediaButton.visibility = View.GONE
                }

                if (!::ad.isInitialized || ad.id == null || !::chatPartnerId.isInitialized) return // Guard clause

                if (!isTyping) {
                    isTyping = true
                    // FIX: Use chatPartnerId
                    WebSocketManager.sendTypingEvent(ad.id!!, chatPartnerId)
                }
                typingJob ?. cancel()
                typingJob = lifecycleScope.launch(Dispatchers.Main) {
                    delay(3000)
                    isTyping = false
                    // FIX: Use chatPartnerId
                    WebSocketManager.sendStopTypingEvent(ad.id!!, chatPartnerId)
                }
            }
            override fun afterTextChanged(s: Editable ?) {}
        })

        // Media Preview Listeners
        closePreviewButton.setOnClickListener {
            hideMediaPreview()
        }
        sendMediaButton.setOnClickListener {
            selectedMediaUri ?. let {
                    uri ->
                val caption = captionEditText.text.toString().trim()
                uploadMedia(uri, caption)
                hideMediaPreview()
            }
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(this, messageList) {
                imageUrl ->
            val intent = Intent(this, FullScreenImageActivity::class.java).apply {
                putStringArrayListExtra("image_urls", arrayListOf(imageUrl))
                putExtra("position", 0)
            }
            startActivity(intent)
        }
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

    private fun observeNewMessages() {
        WebSocketManager.newMessage.observe(this) {
                message ->
            // FIX: Check if the message is for the current conversation thread
            if (::ad.isInitialized && message.advertId == ad.id && (message.senderId == chatPartnerId || message.receiverId == chatPartnerId)) {

                val existingIndex = messageList.indexOfFirst {
                    // Match temporary IDs for messages sent by the current user
                    it.id == "temporaryId" && (it.content == message.content || it.audioUrl != null)
                }

                if (existingIndex != -1) {
                    // Update the local temporary message with the server-generated message
                    messageList[existingIndex] = message
                    messageAdapter.notifyItemChanged(existingIndex)
                } else {
                    // Add the new message only if it's not already in the list (e.g., from the messages history fetch)
                    if (message.id != null && messageList.none { it.id == message.id }) {
                        messageList.add(message)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                    } else if (message.id == null) {
                        messageList.add(message)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                    }
                }
                messagesRecyclerView.scrollToPosition(messageList.size - 1)

                // FIX: Check if the sender is the chat partner, not necessarily the ad owner
                if (message.senderId == chatPartnerId && message.status != "read") {
                    ad.id?.let { WebSocketManager.markMessagesAsRead(it, chatPartnerId) }
                }
            }
        }
    }

    private fun observeMessageStatusUpdates() {
        WebSocketManager.messageStatusUpdate.observe(this) {
                (messageIds, status) ->
            messageIds.forEach {
                    messageId ->
                val index = messageList.indexOfFirst {
                    it.id == messageId
                }
                if (index != -1) {
                    messageList[index] = messageList[index].copy(status = status)
                    messageAdapter.notifyItemChanged(index)
                }
            }
        }
    }

    private fun sendMessage(content: String ?, audioUrl: String ?, receiverId: String) {
        val advertId = ad.id ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(Date())

        val tempMessage = Message(
            id = "temporaryId",
            senderId = myUserId,
            receiverId = receiverId, // This is now chatPartnerId
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

        val payload = mutableMapOf < String,
                Any > (
            "receiver_id" to receiverId, // This is now chatPartnerId
            "advert_id" to advertId,
        )
        content ?. let {
            payload["content"] = it
        }
        audioUrl ?. let {
            payload["audio_url"] = it
        }

        val messagePayload = mapOf("type" to "newMessage", "payload" to payload)
        val jsonMessage = Gson().toJson(messagePayload)
        WebSocketManager.sendMessage(jsonMessage)
    }

    // Updated to use chatPartner
    private fun populateAdAndChatHeader() {
        chatToolbarTitle.text = chatPartner.fullName // Use chatPartner
        chatPartner.profilePictureUrl ?. let { // Use chatPartner
                url ->
            val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + url.trimStart('/')
            Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_profile_placeholder).into(otherUserPhotoInToolbar)
        }
        adTitleTextView.text = ad.title
        adPriceTextView.text = "UGX ${ad.price}"
        val firstPhotoUrl = ad.photos ?. firstOrNull()
        if (firstPhotoUrl != null) {
            val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + firstPhotoUrl.trimStart('/')
            Glide.with(this).load(imageUrl).into(adPhotoImageView)
        } else {
            adPhotoImageView.setImageResource(R.drawable.ic_add_photo)
        }
    }

    // Updated to use chatPartnerId for message fetching
    private fun fetchMessages() {
        if (!::chatPartnerId.isInitialized) return
        val partnerId = chatPartnerId // Use chatPartnerId
        val advertId = ad.id ?: return
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch messages based on Ad ID and the specific chat partner ID
                val response = apiService.getMessages("Bearer $token", advertId, partnerId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val messages = response.body() ?: emptyList()
                        messageList.clear()
                        messageList.addAll(messages)
                        messageAdapter.notifyDataSetChanged()
                        messagesRecyclerView.scrollToPosition(messageList.size - 1)

                        // --- FIX 2: UNCONDITIONALLY mark messages as read on successful chat load ---
                        // This sends the WebSocket message to the backend, which marks them as read
                        // and sends the global unreadCountUpdate back to the MainAppActivity.
                        WebSocketManager.markMessagesAsRead(advertId, partnerId)
                        // --------------------------------------------------------------------------

                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error fetching messages: ${e.message}")
            }
        }
    }

    // Updated to use chatPartnerId for receiver ID
    private fun uploadMedia(uri: Uri, caption: String ?) {
        val token = sessionManager.fetchAuthToken() ?: return
        val advertId = ad.id ?: return
        if (!::chatPartnerId.isInitialized) return
        val receiverId = chatPartnerId // Use chatPartnerId

        getTempFileFromUri(uri) ?. let {
                file ->
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val mediaPart = MultipartBody.Part.createFormData("media", file.name, requestFile)
            val advertIdRequestBody = advertId.toRequestBody("text/plain".toMediaTypeOrNull())
            val receiverIdRequestBody = receiverId.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionRequestBody = caption ?. ifEmpty {
                null
            } ?. toRequestBody("text/plain".toMediaTypeOrNull())

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Correctly use the apiService instance
                    val response = apiService.uploadMessageMedia("Bearer $token", advertIdRequestBody, receiverIdRequestBody, mediaPart, captionRequestBody)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val uploadedMessage = response.body()
                            if (uploadedMessage != null) {
                                // FIX: Rely on observeNewMessages logic to handle addition/update
                                // Since the backend sends a WebSocket message upon successful upload,
                                // the observeNewMessages observer handles inserting the final message.
                                // We don't need explicit message list manipulation here.
                            }
                        } else {
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
            inputStream ?. copyTo(outputStream)
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun observeTypingNotifications() {
        WebSocketManager.typingNotification.observe(this) {
                (senderId, advertId, isTyping) ->
            if (::ad.isInitialized && ::chatPartnerId.isInitialized && senderId == chatPartnerId && advertId == ad.id) { // Use chatPartnerId
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
            typingAnimation ?. start()
        }
    }

    private fun stopTypingAnimation() {
        typingAnimation ?. cancel()
        typingAnimation = null
        typingIndicatorTextView.alpha = 1f
    }

    // --- Audio Recording Functions ---

    private fun checkAndRequestPermissions(): Boolean {
        // Only check for mic permission here, notification permission is handled in MainAppActivity
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array < String > , grantResults: IntArray) {
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
            setOutputFile(audioFile ?. absolutePath)
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
            mediaRecorder ?. stop()
            if (send) {
                audioFile ?. let {
                    uploadAudio(it)
                }
            } else {
                audioFile ?. delete()
            }
        } catch (e: RuntimeException) {
            Log.w("ChatActivity", "MediaRecorder stop() failed", e)
            audioFile ?. delete()
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
        mediaRecorder ?. reset()
        mediaRecorder ?. release()
        mediaRecorder = null
    }

    private fun startTimer() {
        recordTime = 0
        handler.post(object: Runnable {
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

    // Updated to use chatPartnerId for receiver ID
    private fun uploadAudio(file: File) {
        val token = sessionManager.fetchAuthToken() ?: return
        val advertId = ad.id ?: return
        if (!::chatPartnerId.isInitialized) return
        val receiverId = chatPartnerId // Use chatPartnerId

        if (!file.exists() || file.length() == 0L) {
            Log.e("ChatActivity", "Audio file is empty or does not exist.")
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(Date())

        // Use a more unique temporary ID for better tracking
        val tempId = "temporaryId_${UUID.randomUUID()}"

        val tempMessage = Message(
            id = tempId,
            senderId = myUserId,
            receiverId = receiverId,
            advertId = advertId,
            content = null,
            createdAt = timestamp,
            mediaUrl = null,
            audioUrl = file.absolutePath,
            status = "sending"
        )
        val tempMessagePosition = messageList.size
        messageList.add(tempMessage)
        messageAdapter.notifyItemInserted(tempMessagePosition)
        messagesRecyclerView.scrollToPosition(tempMessagePosition)

        val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
        val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestFile)
        val advertIdRequestBody = advertId.toRequestBody("text/plain".toMediaTypeOrNull())
        val receiverIdRequestBody = receiverId.toRequestBody("text/plain".toMediaTypeOrNull())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Correctly use the apiService instance
                val response = apiService.uploadMessageAudio("Bearer $token", advertIdRequestBody, receiverIdRequestBody, audioPart)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val uploadedMessage = response.body()
                        if (uploadedMessage != null) {
                            // Find the temporary message using the unique tempId
                            val index = messageList.indexOfFirst { it.id == tempId }
                            if (index != -1) {
                                messageList[index] = uploadedMessage
                                messageAdapter.notifyItemChanged(index)
                            }
                        }
                    } else {
                        val index = messageList.indexOfFirst { it.id == tempId }
                        if (index != -1) {
                            messageList[index] = tempMessage.copy(status = "failed")
                            messageAdapter.notifyItemChanged(index)
                        }
                        Toast.makeText(this@ChatActivity, "Failed to send audio", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val index = messageList.indexOfFirst { it.id == tempId }
                    if (index != -1) {
                        messageList[index] = tempMessage.copy(status = "failed")
                        messageAdapter.notifyItemChanged(index)
                    }
                    Toast.makeText(this@ChatActivity, "Error sending audio: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}