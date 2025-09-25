package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.ConversationsAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.ConversationSummary
import com.lonyitrade.app.utils.NetworkUtils
import com.lonyitrade.app.utils.SessionManager
import com.lonyitrade.app.utils.WebSocketManager
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {

    // Nullable view properties to handle the fragment's view lifecycle
    private var conversationsRecyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null

    private lateinit var conversationsAdapter: ConversationsAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)

        // Initialize views immediately after the layout is inflated
        conversationsRecyclerView = view.findViewById(R.id.conversationsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        fetchConversations()
        observeWebSocketUpdates()
    }

    private fun setupRecyclerView() {
        conversationsAdapter = ConversationsAdapter(mutableListOf())
        // Use the safe-call ?. to access the nullable view
        conversationsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            // Corrected the typo here
            adapter = conversationsAdapter
        }
    }

    private fun observeWebSocketUpdates() {
        WebSocketManager.newMessage.observe(viewLifecycleOwner) { message ->
            val conversation = conversationsAdapter.getConversations().find { conv ->
                conv.advertId == message.advertId && conv.otherUserId == message.senderId
            }
            if (conversation != null) {
                val updatedConversation = conversation.copy(
                    lastMessage = message.content,
                    unreadCount = conversation.unreadCount + 1
                )
                conversationsAdapter.updateOrAddConversation(updatedConversation)
            } else {
                fetchConversations()
            }
        }
        WebSocketManager.messageStatusUpdate.observe(viewLifecycleOwner) {
            fetchConversations()
        }
        WebSocketManager.typingNotification.observe(viewLifecycleOwner) { (senderId, advertId, isTyping) ->
            val myUserId = sessionManager.getUserId()
            if (senderId != myUserId) {
                conversationsAdapter.updateTypingStatus(advertId, senderId, isTyping)
            }
        }
    }

    private fun fetchConversations() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar?.visibility = View.VISIBLE
        val token = sessionManager.fetchAuthToken()

        if (token == null) {
            progressBar?.visibility = View.GONE
            Toast.makeText(context, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getConversations("Bearer $token")
                if (response.isSuccessful) {
                    conversationsAdapter.updateConversations(response.body() ?: emptyList())
                } else {
                    Toast.makeText(context, "Failed to load conversations: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching conversations: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up view references to prevent memory leaks
        conversationsRecyclerView = null
        progressBar = null
    }
}