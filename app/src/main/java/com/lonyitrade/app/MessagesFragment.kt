package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.ConversationsAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.utils.NetworkUtils
import com.lonyitrade.app.utils.SessionManager
import com.lonyitrade.app.utils.WebSocketManager
import com.lonyitrade.app.viewmodels.SharedViewModel
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {

    private var conversationsRecyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null

    private lateinit var conversationsAdapter: ConversationsAdapter
    private lateinit var sessionManager: SessionManager
    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)

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
        conversationsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = conversationsAdapter
        }
    }

    private fun observeWebSocketUpdates() {
        // A simple and robust way to stay in sync is to re-fetch conversations on any update.
        WebSocketManager.newMessage.observe(viewLifecycleOwner) {
            fetchConversations()
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
                // Correctly use the apiService instance
                val response = apiService.getConversations("Bearer $token")
                if (response.isSuccessful) {
                    val conversations = response.body() ?: emptyList()
                    conversationsAdapter.updateConversations(conversations)

                    // Calculate the total unread count and update the SharedViewModel
                    val totalUnread = conversations.sumOf { it.unreadCount }
                    sharedViewModel.setUnreadMessageCount(totalUnread)

                } else {
                    Toast.makeText(context, "Failed to load conversations: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
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
        conversationsRecyclerView = null
        progressBar = null
    }
}