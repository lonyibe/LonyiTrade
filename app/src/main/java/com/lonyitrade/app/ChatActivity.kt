package com.lonyitrade.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lonyitrade.app.adapters.MessageAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.Message
import com.lonyitrade.app.data.models.UserProfile
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var ad: Ad
    private lateinit var seller: UserProfile
    private lateinit var myUserId: String
    private var messageList: MutableList<Message> = mutableListOf()
    private lateinit var messageAdapter: MessageAdapter

    // UI Elements
    private lateinit var adTitleTextView: TextView
    private lateinit var adPriceTextView: TextView
    private lateinit var adPhotoImageView: ImageView
    private lateinit var chatToolbarTitle: TextView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        sessionManager = SessionManager(this)
        ad = intent.getParcelableExtra("AD_EXTRA") ?: run {
            Toast.makeText(this, "Error loading ad data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        myUserId = sessionManager.fetchAuthToken() ?: ""

        initializeViews()
        setupListeners()
        setupRecyclerView()
        fetchSellerDetails()
    }

    private fun initializeViews() {
        // Toolbar views
        chatToolbarTitle = findViewById(R.id.chatToolbarTitle)
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        // Ad Card views
        adTitleTextView = findViewById(R.id.adTitleTextView)
        adPriceTextView = findViewById(R.id.adPriceTextView)
        adPhotoImageView = findViewById(R.id.adPhotoImageView)

        // Chat views
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
    }

    private fun setupListeners() {
        sendButton.setOnClickListener {
            val content = messageEditText.text.toString().trim()
            val sellerUserId = ad.userId
            if (content.isNotEmpty() && sellerUserId != null) {
                sendMessage(content, sellerUserId)
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

    private fun fetchSellerDetails() {
        val sellerUserId = ad.userId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getUserById("Bearer $myUserId", sellerUserId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        seller = response.body() ?: return@withContext
                        populateAdAndChatHeader()
                        fetchMessages()
                    } else {
                        Toast.makeText(this@ChatActivity, "Failed to load seller details", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateAdAndChatHeader() {
        chatToolbarTitle.text = seller.fullName
        adTitleTextView.text = ad.title
        adPriceTextView.text = "UGX ${ad.price}"

        // Use safe call operator for nullable photo list
        val firstPhotoUrl = ad.photos?.firstOrNull()
        if (firstPhotoUrl != null) {
            val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + firstPhotoUrl.trimStart('/')
            Glide.with(this)
                .load(imageUrl)
                .into(adPhotoImageView)
        } else {
            adPhotoImageView.setImageResource(R.drawable.ic_add_photo)
        }
    }

    private fun fetchMessages() {
        val sellerUserId = ad.userId ?: return
        val advertId = ad.id ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getMessages("Bearer $myUserId", advertId, sellerUserId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val messages = response.body() ?: emptyList()
                        messageList.clear()
                        messageList.addAll(messages)
                        messageAdapter.notifyDataSetChanged()
                        messagesRecyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun sendMessage(content: String, receiverId: String) {
        val advertId = ad.id ?: return
        val message = Message("temp_id", myUserId, receiverId, advertId, content, "")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // The Message data class needs an advertId and the postMessage API expects it in the body.
                // The current implementation of ApiService.postMessage takes a Message object.
                // It looks like the Message data class needs to be updated to make the advertId nullable.
                val response = ApiClient.apiService.postMessage("Bearer $myUserId", message)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        messageEditText.text.clear()
                        fetchMessages() // Refresh messages to get the new one
                    } else {
                        Toast.makeText(this@ChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error sending message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}