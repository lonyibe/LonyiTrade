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
    private lateinit var seller: UserProfile
    private lateinit var myUserId: String
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
        checkAndRequestPermissions() // Permissions for mic/audio recording

        // FIX: Delegate the actual intent processing to a dedicated method
        handleIncomingIntent(intent)

        // FIX: Ensure real-time observers are set up immediately
        observeTypingNotifications()
        observeNewMessages()
        observeMessageStatusUpdates()
    }

    // FIX: Correct signature for onNewIntent to properly override the method, addressing the 'overrides nothing' error.
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // FIX: Set the new intent as the current one and handle it if it's not null.
        if (intent != null) {
            setIntent(intent)
            handleIncomingIntent(intent)
        }
    }

    // FIX: Extracted intent handling logic into a reusable method.
    private fun handleIncomingIntent(intent: Intent) {
        val adParcelable = intent.getParcelableExtra("AD_EXTRA") as? Ad
        val adIdFromNotification = intent.getStringExtra("adId")

        if (adParcelable != null) {
            // Case 1: Launched from MessagesFragment/AdDetailActivity (expected)
            ad = adParcelable
            if (ad.userId == null) { // Check for a critical piece of data
                Toast.makeText(this, "Seller details are missing. Cannot start chat.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            fetchSellerDetails() // Will call fetchMessages() on success
        } else if (adIdFromNotification != null) {
            // Case 2: Launched from Notification (deep link)
            // The Firebase Service passed the adId, now we fetch the full Ad object.
            fetchAdDetailsForDeepLink(adIdFromNotification)
        } else {
            // Case 3: Error (neither parcelable nor adId found) - Only exit if it's the first launch
            if (!::ad.isInitialized) {
                Toast.makeText(this, "Error: Missing conversation details.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    // --- NEW FUNCTION: Fetch Ad Details for Deep Link ---
    private fun fetchAdDetailsForDeepLink(adId: String) {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            Toast.makeText(this, "Please log in to view this conversation.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val adResponse = apiService.getAdvertById("Bearer $token", adId)

                if (!adResponse.isSuccessful || adResponse.body() == null) {
                    throw IOException("Failed to fetch Ad details for ID: $adId. HTTP Code: ${adResponse.code()}")
                }

                val fetchedAd = adResponse.body()!!

                if (fetchedAd.userId == null) {
                    throw IOException("Fetched Ad is missing seller ID.")
                }

                // Set the fetched Ad and proceed
                withContext(Dispatchers.Main) {
                    ad = fetchedAd
                    fetchSellerDetails() // Will proceed with chat setup
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Deep link failed to fetch Ad: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error loading conversation details. Please try again.", Toast.LENGTH_LONG).show()
                    // Only finish if 'ad' wasn't initialized, otherwise keep the user in the existing chat context
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
                    ad.userId?.let { receiverId ->
                        WebSocketManager.sendStopTypingEvent(advertId, receiverId)
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

                if (!::ad.isInitialized || ad.id == null || ad.userId == null) return // Guard clause

                if (!isTyping) {
                    isTyping = true
                    WebSocketManager.sendTypingEvent(ad.id!!, ad.userId!!)
                }
                typingJob ?. cancel()
                typingJob = lifecycleScope.launch(Dispatchers.Main) {
                    delay(3000)
                    isTyping = false
                    WebSocketManager.sendStopTypingEvent(ad.id!!, ad.userId!!)
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

    private fun fetchSellerDetails() {
        val sellerUserId = ad.userId ?: return
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Correctly use the apiService instance
                val response = apiService.getUserById("Bearer $token", sellerUserId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        seller = response.body() !!
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
        WebSocketManager.newMessage.observe(this) {
                message ->
            if (::ad.isInitialized && message.advertId == ad.id && (message.senderId == myUserId || message.receiverId == myUserId)) {
                val existingIndex = messageList.indexOfFirst {
                    it.id == "temporaryId" && (it.content == message.content || it.audioUrl != null)
                }
                if (existingIndex != -1) {
                    messageList[existingIndex] = message
                    messageAdapter.notifyItemChanged(existingIndex)
                } else {
                    if (message.id != null && messageList.none { it.id == message.id }) {
                        messageList.add(message)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                    } else if (message.id == null) {
                        messageList.add(message)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                    }
                }
                messagesRecyclerView.scrollToPosition(messageList.size - 1)
                if (message.senderId == ad.userId && message.status != "read") {
                    ad.id?.let { ad.userId?.let { it1 -> WebSocketManager.markMessagesAsRead(it, it1) } }
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

        val payload = mutableMapOf < String,
                Any > (
            "receiver_id" to receiverId,
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

    private fun populateAdAndChatHeader() {
        chatToolbarTitle.text = seller.fullName
        seller.profilePictureUrl ?. let {
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

    private fun fetchMessages() {
        val sellerUserId = ad.userId ?: return
        val advertId = ad.id ?: return
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Correctly use the apiService instance
                val response = apiService.getMessages("Bearer $token", advertId, sellerUserId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val messages = response.body() ?: emptyList()
                        messageList.clear()
                        messageList.addAll(messages)
                        messageAdapter.notifyDataSetChanged()
                        messagesRecyclerView.scrollToPosition(messageList.size - 1)
                        if (messages.any {
                                it.receiverId == myUserId && it.status != "read"
                            }) {
                            WebSocketManager.markMessagesAsRead(advertId, sellerUserId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error fetching messages: ${e.message}")
            }
        }
    }

    private fun uploadMedia(uri: Uri, caption: String ?) {
        val token = sessionManager.fetchAuthToken() ?: return
        val advertId = ad.id ?: return
        val receiverId = ad.userId ?: return

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
                                messageList.add(uploadedMessage)
                                messageAdapter.notifyItemInserted(messageList.size - 1)
                                messagesRecyclerView.scrollToPosition(messageList.size - 1)
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
            if (::ad.isInitialized && senderId == ad.userId && advertId == ad.id) {
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

    private fun uploadAudio(file: File) {
        val token = sessionManager.fetchAuthToken() ?: return
        val advertId = ad.id ?: return
        val receiverId = ad.userId ?: return

        if (!file.exists() || file.length() == 0L) {
            Log.e("ChatActivity", "Audio file is empty or does not exist.")
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(Date())
        val tempMessage = Message(
            id = "temporaryId_${System.currentTimeMillis()}",
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
                            val index = messageList.indexOfFirst { it.id == tempMessage.id }
                            if (index != -1) {
                                messageList[index] = uploadedMessage
                                messageAdapter.notifyItemChanged(index)
                            }
                        }
                    } else {
                        val index = messageList.indexOfFirst { it.id == tempMessage.id }
                        if (index != -1) {
                            messageList[index] = tempMessage.copy(status = "failed")
                            messageAdapter.notifyItemChanged(index)
                        }
                        Toast.makeText(this@ChatActivity, "Failed to send audio", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val index = messageList.indexOfFirst { it.id == tempMessage.id }
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