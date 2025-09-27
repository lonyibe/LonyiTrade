package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.NotificationReviewAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.ReviewNotification
import com.lonyitrade.app.utils.LoadingDialogFragment
import com.lonyitrade.app.utils.SessionManager
import com.lonyitrade.app.viewmodels.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsActivity : AppCompatActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var noNotificationsTextView: TextView
    private lateinit var notificationAdapter: NotificationReviewAdapter

    private lateinit var sessionManager: SessionManager
    private val apiService by lazy { ApiClient().getApiService(this) }
    private val loadingDialog = LoadingDialogFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        sessionManager = SessionManager(this)

        // Set up the simple toolbar
        findViewById<TextView>(R.id.toolbarTitle).text = "Notifications"
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Initialize content views
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        noNotificationsTextView = findViewById(R.id.noNotificationsTextView)

        setupRecyclerView()
        fetchNotifications()
    }

    /**
     * Set up the RecyclerView with the custom adapter and a click listener to navigate to the ReviewActivity.
     */
    private fun setupRecyclerView() {
        notificationAdapter = NotificationReviewAdapter(emptyList()) { reviewNotification ->
            // FIX: Navigate to the ReviewActivity for the corresponding advert.
            val intent = Intent(this, ReviewActivity::class.java).apply {
                putExtra("adId", reviewNotification.advert_id) // Pass advertId for deep-linking
            }
            startActivity(intent)
        }
        notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationAdapter
        }
    }


    /**
     * Fetches the list of unread reviews, updates the list, and marks them as read.
     */
    private fun fetchNotifications() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please log in to view notifications.", Toast.LENGTH_SHORT).show()
            noNotificationsTextView.visibility = View.VISIBLE
            return
        }

        if (!loadingDialog.isAdded) {
            loadingDialog.show(supportFragmentManager, "loading")
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch the list of UNREAD review notifications
                val response = apiService.getUnreadReviews("Bearer $token")

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        val reviewNotifications = response.body()!!

                        notificationAdapter.updateReviews(reviewNotifications)

                        if (reviewNotifications.isNotEmpty()) {
                            // 2. Display the list
                            notificationsRecyclerView.visibility = View.VISIBLE
                            noNotificationsTextView.visibility = View.GONE

                            // 3. CRITICAL: Mark the reviews as read and reset the counter on the backend
                            markNotificationsAsRead("Bearer $token")
                        } else {
                            // No unread reviews
                            notificationsRecyclerView.visibility = View.GONE
                            noNotificationsTextView.visibility = View.VISIBLE
                        }
                    } else {
                        Log.e("NotificationsActivity", "Failed to fetch reviews: ${response.code()}")
                        Toast.makeText(this@NotificationsActivity, "Failed to load notifications.", Toast.LENGTH_SHORT).show()
                        notificationsRecyclerView.visibility = View.GONE
                        noNotificationsTextView.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Log.e("NotificationsActivity", "Error fetching notifications", e)
                    Toast.makeText(this@NotificationsActivity, "Network error fetching notifications.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Calls the API to mark all displayed review notifications as read.
     */
    private fun markNotificationsAsRead(token: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // This API call internally resets the review count to 0 and broadcasts the WebSocket update.
                val response = apiService.markReviewsAsRead(token)
                if (response.isSuccessful) {
                    Log.d("NotificationsActivity", "All reviews marked as read. Counter should reset.")
                    // The SharedViewModel observer in MainAppActivity will handle setting the count to 0.
                } else {
                    Log.e("NotificationsActivity", "Failed to mark reviews as read: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NotificationsActivity", "Error marking reviews as read", e)
            }
        }
    }
}