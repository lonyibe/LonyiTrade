package com.lonyitrade.app.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Message
import com.lonyitrade.app.viewmodels.SharedViewModel
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min

object WebSocketManager {

    var webSocket: WebSocket? = null
    private var isConnected = false
    private lateinit var client: OkHttpClient
    private lateinit var request: Request
    private var token: String? = null

    // For automatic reconnection
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private var reconnectAttempts = 0
    private const val MAX_RECONNECT_INTERVAL = 30000L // 30 seconds

    // LiveData for connection status
    private val _connectionStatus = MutableLiveData<Boolean>()
    val connectionStatus: LiveData<Boolean> = _connectionStatus

    // LiveData to hold new incoming messages. The chat activity can observe this.
    private val _newMessage = MutableLiveData<Message>()
    val newMessage: LiveData<Message> = _newMessage

    // LiveData for unread count updates (DEPRECATED: Now handled via SharedViewModel total)
    private val _unreadCountUpdate = MutableLiveData<Int>()
    val unreadCountUpdate: LiveData<Int> = _unreadCountUpdate

    // LiveData for typing notifications
    private val _typingNotification = MutableLiveData<Triple<String, String, Boolean>>()
    val typingNotification: LiveData<Triple<String, String, Boolean>> = _typingNotification

    // LiveData for message status updates (sent, delivered, read)
    private val _messageStatusUpdate = MutableLiveData<Pair<List<String>, String>>()
    val messageStatusUpdate: LiveData<Pair<List<String>, String>> = _messageStatusUpdate

    // NEW: Reference to the shared ViewModel
    private var sharedViewModel: SharedViewModel? = null

    private val gson = Gson()

    // NEW: Method to set the shared ViewModel from the MainAppActivity
    fun setSharedViewModel(viewModel: SharedViewModel) {
        this.sharedViewModel = viewModel
    }

    fun connect(authToken: String) {
        if (isConnected) {
            Log.d("WebSocketManager", "WebSocket is already connected.")
            return
        }
        this.token = authToken

        // It's good practice to keep the pingInterval here as a client-side safety net
        client = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS) // Keep the connection alive
            .build()

        val wsUrl = ApiClient.BASE_URL
            .replace("http", "ws")
            .trimEnd('/') + "?token=$token"

        request = Request.Builder()
            .url(wsUrl)
            .build()

        Log.d("WebSocketManager", "Attempting to connect to $wsUrl")
        webSocket = client.newWebSocket(request, WebSocketListenerImpl())
    }

    private class WebSocketListenerImpl : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            isConnected = true
            reconnectAttempts = 0 // Reset reconnect attempts on successful connection
            _connectionStatus.postValue(true)
            Log.d("WebSocketManager", "WebSocket connection opened: ${response.message}")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocketManager", "Receiving: $text")
            handleIncomingMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocketManager", "Closing: $code / $reason")
            isConnected = false
            _connectionStatus.postValue(false)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WebSocketManager", "Error: ${t.message}", t)
            isConnected = false
            _connectionStatus.postValue(false)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= 5) {
            Log.d("WebSocketManager", "Max reconnect attempts reached. Will not try again automatically.")
            return
        }
        val delay = min((1000 * (Math.pow(2.0, reconnectAttempts.toDouble()))).toLong(), MAX_RECONNECT_INTERVAL)
        Log.d("WebSocketManager", "Scheduling reconnect in ${delay}ms")
        reconnectHandler.postDelayed({
            reconnectAttempts++
            token?.let { connect(it) }
        }, delay)
    }

    fun disconnect() {
        if (webSocket != null) {
            webSocket?.close(1000, "User disconnected")
            webSocket = null
            isConnected = false
            reconnectHandler.removeCallbacksAndMessages(null) // Cancel any pending reconnects
            _connectionStatus.postValue(false)
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val jsonObject = JSONObject(text)
            when (jsonObject.getString("type")) {
                "unreadCountUpdate" -> {
                    val messageCount = jsonObject.getJSONObject("payload").getInt("unreadCount")
                    // Use the shared ViewModel to update the unread message count
                    sharedViewModel?.setUnreadMessageCount(messageCount)
                }
                // FIX 1: Handle new review count updates from WebSocket
                "reviewCountUpdate" -> {
                    val reviewCount = jsonObject.getJSONObject("payload").getInt("reviewCount")
                    sharedViewModel?.setUnreadReviewCount(reviewCount)
                }
                "newMessage" -> {
                    val message = gson.fromJson(jsonObject.getJSONObject("payload").toString(), Message::class.java)
                    _newMessage.postValue(message)
                }
                "typingNotification" -> {
                    val payload = jsonObject.getJSONObject("payload")
                    val senderId = payload.getString("senderId")
                    val advertId = payload.getString("advertId")
                    val isTyping = payload.getBoolean("isTyping")
                    _typingNotification.postValue(Triple(senderId, advertId, isTyping))
                }
                "messageStatusUpdate" -> {
                    val payload = jsonObject.getJSONObject("payload")
                    val status = payload.getString("status")
                    val messageIds = mutableListOf<String>()
                    val messageIdsArray = payload.optJSONArray("messageIds")
                    if (messageIdsArray != null) {
                        for (i in 0 until messageIdsArray.length()) {
                            messageIds.add(messageIdsArray.getString(i))
                        }
                    }
                    _messageStatusUpdate.postValue(Pair(messageIds, status))
                }
            }
        } catch (e: JsonSyntaxException) {
            Log.e("WebSocketManager", "Failed to parse JSON: ${e.message}")
        } catch (e: JSONException) {
            Log.e("WebSocketManager", "Failed to parse JSON: ${e.message}")
        }
    }


    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun sendTypingEvent(advertId: String, receiverId: String) {
        val payload = mapOf("advertId" to advertId, "receiverId" to receiverId)
        val messagePayload = mapOf("type" to "typing", "payload" to payload)
        val jsonMessage = gson.toJson(messagePayload)
        webSocket?.send(jsonMessage)
    }

    fun sendStopTypingEvent(advertId: String, receiverId: String) {
        val payload = mapOf("advertId" to advertId, "receiverId" to receiverId)
        val messagePayload = mapOf("type" to "stopTyping", "payload" to payload)
        val jsonMessage = gson.toJson(messagePayload)
        webSocket?.send(jsonMessage)
    }

    fun markMessagesAsRead(advertId: String, otherUserId: String) {
        val payload = mapOf("advertId" to advertId, "otherUserId" to otherUserId)
        val messagePayload = mapOf("type" to "markAsRead", "payload" to payload)
        val jsonMessage = gson.toJson(messagePayload)
        webSocket?.send(jsonMessage)
    }
}