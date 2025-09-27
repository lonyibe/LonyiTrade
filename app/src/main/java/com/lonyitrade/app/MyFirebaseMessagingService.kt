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
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.FcmTokenRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var sessionManager: SessionManager
    private val apiService: ApiService by lazy { ApiClient().getApiService(applicationContext) }

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

            val type = remoteMessage.data["type"]
            val adId = remoteMessage.data["adId"]
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]

            if (type == "newMessage" && adId != null) {
                // FIX: If a message is received, we need to fetch the full Ad object for the deep link
                handleMessageNotification(adId, title, body, remoteMessage.data)
            } else {
                // For general notifications (like 'newReview' or others)
                showNotification(title, body, remoteMessage.data, null)
            }
        }
    }

    /**
     * Fetches Ad data and then displays the notification with a specific deep link.
     */
    private fun handleMessageNotification(adId: String, title: String?, body: String?, data: Map<String, String>) {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            // Can't fetch details without a token, so just open the main activity
            showNotification(title, body, data, null)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getAdvertById("Bearer $token", adId)
                if (response.isSuccessful) {
                    val ad = response.body()
                    if (ad != null) {
                        // FIX: Pass the full Ad object to the notification creator
                        showNotification(title, body, data, ad)
                    } else {
                        Log.e("FCM", "Failed to parse Ad data for deep link.")
                        showNotification(title, body, data, null)
                    }
                } else {
                    Log.e("FCM", "API failed to fetch Ad details for notification: ${response.errorBody()?.string()}")
                    showNotification(title, body, data, null)
                }
            } catch (e: Exception) {
                Log.e("FCM", "Network error while fetching Ad details for notification: ${e.message}")
                showNotification(title, body, data, null)
            }
        }
    }


    /**
     * Creates and displays a system notification.
     * The targetIntent now depends on whether an Ad object is provided.
     */
    private fun showNotification(title: String?, body: String?, data: Map<String, String>, ad: Ad?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "default_channel_id"

        // Create a notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // --- FIX: Change Target Activity and Pass Parcelable ---
        val targetIntent = if (ad != null) {
            // Deep Link to ChatActivity with the required Ad object
            Intent(this, ChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // This is CRUCIAL: Pass the fetched Ad object as the expected Parcelable extra
                putExtra("AD_EXTRA", ad)
                // You can still pass the raw data, but the Ad object is what ChatActivity needs
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }
        } else {
            // Fallback to SplashActivity or MainAppActivity (no deep link)
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

        // Use a unique ID for chat notifications to update them later if needed.
        // For simplicity, using a generic ID or adId hash here.
        // Using 0 as before, but this is a point for future improvement.
        val notificationId = ad?.id?.hashCode() ?: 0

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
                // Use the existing apiService instance
                val response = apiService.updateFcmToken(FcmTokenRequest(token))

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