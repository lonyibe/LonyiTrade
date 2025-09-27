package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.viewmodels.SharedViewModel

class NotificationsActivity : AppCompatActivity() {

    // Inject the shared ViewModel instance that holds the aggregated notification count
    // This assumes the ViewModel is scoped correctly (e.g., to the application or activity group)
    private val sharedViewModel: SharedViewModel by viewModels()

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var noNotificationsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Set up the simple toolbar for this new activity
        findViewById<TextView>(R.id.toolbarTitle).text = "Notifications"
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Initialize content views
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        noNotificationsTextView = findViewById(R.id.noNotificationsTextView)

        // Note: RecyclerView requires a LayoutManager and an Adapter to be fully functional.
        // notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        // notificationsRecyclerView.adapter = NotificationAdapter(...) // TODO: Create and initialize adapter

        // FIX: Observe the total notification count to manage empty state visibility
        sharedViewModel.totalNotificationCount.observe(this) { count ->
            if (count != null && count > 0) {
                // Has notifications: show list, hide empty view
                notificationsRecyclerView.visibility = View.VISIBLE
                noNotificationsTextView.visibility = View.GONE
                // Note: You would typically update the RecyclerView adapter here with the full list of notifications.
            } else {
                // No notifications: hide list, show empty view
                notificationsRecyclerView.visibility = View.GONE
                noNotificationsTextView.visibility = View.VISIBLE
            }
        }
    }
}