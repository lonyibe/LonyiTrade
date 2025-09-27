package com.lonyitrade.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.FcmTokenRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var sessionManager: SessionManager
    private val CHAT_CHANNEL_ID = "default_channel_id"

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
        createNotificationChannel()
    }

    /**
     * Creates the notification channel for Android Oreo and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHAT_CHANNEL_ID, "LonyiTrade Chat Messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications for incoming chat messages."
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Called when a new FCM token is generated.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        sendTokenToServer(token)
    }

    /**
     * Called when a new notification is received.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "From: ${remoteMessage.from}")

        // Handle data payload (all message content)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Message data payload: " + remoteMessage.data)

            val title = remoteMessage.data["title"]
            var body = remoteMessage.data["body"] // Start with the backend's default body

            // Logic to customize the body for chat messages
            val adId = remoteMessage.data["adId"]
            val senderName = remoteMessage.data["senderName"]
            val messageContent = remoteMessage.data["messageContent"]
            val otherUserId = remoteMessage.data["otherUserId"]

            if (adId != null && senderName != null && messageContent != null && otherUserId != null) {
                // Use the specific name and message content for better preview
                body = "$senderName: $messageContent"
            }

            showNotification(title, body, remoteMessage.data)
        }
    }

    /**
     * Creates and displays a system notification.
     * FIX: Uses TaskStackBuilder to ensure proper navigation stack (MainAppActivity -> ChatActivity).
     */
    private fun showNotification(title: String?, body: String?, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val adId = data["adId"]
        val otherUserId = data["otherUserId"] // Critical for ChatActivity deep link logic

        // 1. Create an Intent for the MainAppActivity (the parent)
        val mainAppIntent = Intent(this, MainAppActivity::class.java)

        // 2. Create the Intent for the target ChatActivity
        val chatIntent = if (adId != null && otherUserId != null) {
            Intent(this, ChatActivity::class.java).apply {
                // Pass all data to ChatActivity.onNewIntent/handleIncomingIntent
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }
                // Setting flags helps ensure the activity gets the new intent,
                // but TaskStackBuilder is preferred for stack creation.
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP // Better for bringing existing chat to front
            }
        } else {
            // Fallback: If chat context is missing, go to the main app screen
            Intent(this, MainAppActivity::class.java)
        }

        // 3. Build the Task Stack
        val pendingIntent = if (adId != null && otherUserId != null) {
            TaskStackBuilder.create(this).run {
                // Add MainAppActivity as the parent activity
                addNextIntentWithParentStack(mainAppIntent)
                // Add ChatActivity as the top activity
                addNextIntent(chatIntent)

                // FLAG_UPDATE_CURRENT ensures the extras are passed
                getPendingIntent(adId.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
        } else {
            // Use a simple pending intent for non-chat notifications
            PendingIntent.getActivity(this, 0, chatIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationId = adId?.hashCode() ?: System.currentTimeMillis().toInt() // Use a reliable ID

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Sends the new FCM token to your backend server.
     */
    private fun sendTokenToServer(token: String) {
        // Only send the token if the user is logged in
        if (sessionManager.fetchAuthToken() == null) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiClient = ApiClient()
                // FIX: Ensure API Key is passed correctly if necessary, though it seems implicit here.
                val response = apiClient.getApiService(this@MyFirebaseMessagingService).updateFcmToken(
                    // FIX: Ensure the API endpoint correctly handles the user context via the token/auth header
                    FcmTokenRequest(token)
                )

                if (response.isSuccessful) {
                    Log.d("FCM", "Token updated successfully on server.")
                } else {
                    // Log HTTP code for better debugging
                    Log.e("FCM", "Failed to update token on server. Code: ${response.code()}, Body: ${response.errorBody()?.string()}")
                }
            } catch (e: IOException) {
                Log.e("FCM", "Network error while sending token: ${e.message}")
            } catch (e: HttpException) {
                Log.e("FCM", "HTTP error while sending token: ${e.message}")
            }
        }
    }
}
