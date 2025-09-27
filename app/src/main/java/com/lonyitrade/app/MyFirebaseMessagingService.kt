package com.lonyitrade.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.api.ApiService
import com.lonyitrade.app.data.models.FcmTokenRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
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

        // Log the message details
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d("FCM", "Message data payload: " + remoteMessage.data)
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]

            // FIX: Pass all data to a simpler showNotification that creates intent for ChatActivity
            showNotification(title, body, remoteMessage.data)
        }
    }

    /**
     * Creates and displays a system notification.
     * Always passes data to ChatActivity, which is now responsible for fetching the Ad object.
     */
    private fun showNotification(title: String?, body: String?, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "default_channel_id"

        // Create a notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // --- FIX: Determine Target Activity Based on Ad ID ---
        val adId = data["adId"]

        // If the message has an adId (i.e., it's a chat notification)
        val targetIntent = if (adId != null) {
            Intent(this, ChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Pass the adId and other data as simple strings
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }
        } else {
            // Fallback to SplashActivity for general notifications (like reviews)
            Intent(this, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = adId?.hashCode() ?: 0 // Use adId for unique ID

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
                val response = apiClient.getApiService(this@MyFirebaseMessagingService).updateFcmToken(FcmTokenRequest(token))

                if (response.isSuccessful) {
                    Log.d("FCM", "Token updated successfully on server.")
                } else {
                    Log.e("FCM", "Failed to update token on server: ${response.errorBody()?.string()}")
                }
            } catch (e: IOException) {
                Log.e("FCM", "Network error while sending token: ${e.message}")
            } catch (e: HttpException) {
                Log.e("FCM", "HTTP error while sending token: ${e.message}")
            }
        }
    }
}