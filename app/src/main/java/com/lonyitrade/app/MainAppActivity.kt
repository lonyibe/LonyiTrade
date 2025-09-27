// File: app/src/main/java/com/lonyitrade/app/MainAppActivity.kt
package com.lonyitrade.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.messaging.FirebaseMessaging
import com.lonyitrade.app.adapters.MainAppPagerAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.FcmTokenRequest
import com.lonyitrade.app.utils.SessionManager
import com.lonyitrade.app.utils.WebSocketManager
import com.lonyitrade.app.viewmodels.SharedViewModel
// FIX 1: Import the new NotificationCountsResponse model
import com.lonyitrade.app.data.models.NotificationCountsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainAppActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var headerTabLayout: TabLayout
    private lateinit var searchIcon: ImageView
    private lateinit var addListingIcon: ImageView
    private lateinit var notificationIcon: ImageView // Kept for referencing the ImageView inside the FrameLayout
    // FIX 2: New UI component declarations
    private lateinit var notificationIconContainer: FrameLayout
    private lateinit var notificationBadgeTextView: TextView
    private lateinit var headerTitleTextView: TextView
    private lateinit var backButtonIcon: ImageView
    private lateinit var sessionManager: SessionManager
    private val sharedViewModel: SharedViewModel by viewModels()

    // ... (requestPermissionLauncher remains the same) ...

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        sessionManager = SessionManager(this)

        viewPager = findViewById(R.id.main_view_pager)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        headerTabLayout = findViewById(R.id.header_tab_layout)
        searchIcon = findViewById(R.id.search_icon)
        addListingIcon = findViewById(R.id.add_listing_icon)

        // FIX 3: Initialize all header UI components, including the new badge elements
        notificationIconContainer = findViewById(R.id.notification_icon_container)
        notificationIcon = findViewById(R.id.notification_icon)
        notificationBadgeTextView = findViewById(R.id.notification_badge_text_view)

        headerTitleTextView = findViewById(R.id.header_title_text_view)
        backButtonIcon = findViewById(R.id.back_button_icon)

        // Pass the sharedViewModel to the WebSocketManager first
        WebSocketManager.setSharedViewModel(sharedViewModel)
        // Then connect with the token
        sessionManager.fetchAuthToken()?.let { token ->
            WebSocketManager.connect(token)
            // FIX: Fetch the initial total unread count on startup
            fetchInitialNotificationCount()
        }

        // --- FIX: Call permission check on activity creation ---
        checkNotificationPermission()

        // --- New FCM Token Logic ---
        retrieveAndSendFcmToken()

        viewPager.adapter = MainAppPagerAdapter(this)
        viewPager.offscreenPageLimit = 4 // Keep all fragments in memory

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavigationView.menu.getItem(position).isChecked = true
                updateHeaderForPosition(position)
            }
        })

        bottomNavigationView.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_rentals -> 1
                R.id.nav_post_ad -> 2
                R.id.nav_messages -> 3
                R.id.nav_my_account -> 4
                else -> 0
            }
            viewPager.setCurrentItem(position, false) // Disable smooth scroll for instant switching
            true
        }

        bottomNavigationView.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_home) {
                val currentFragment = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
                // Ensure correct fragment casting if resetting logic is needed
                // if (currentFragment is HomeFragment) { currentFragment.resetToHomePage() }
            }
        })

        headerTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val currentFragment = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
                val sortBy = when (tab?.position) {
                    0 -> "top"
                    1 -> "latest"
                    else -> "latest"
                }
                // Ensure correct fragment casting and method calling
                // if (currentFragment is HomeFragment) { currentFragment.fetchAllAdverts(sortBy = sortBy) }
                // else if (currentFragment is RentalsFragment) { currentFragment.fetchRentals(sortBy = sortBy) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        if (savedInstanceState == null) {
            updateHeaderForPosition(0)
        }

        searchIcon.setOnClickListener {
            val intent = if (viewPager.currentItem == 1) {
                Intent(this, SearchRentalActivity::class.java)
            } else {
                Intent(this, SearchActivity::class.java)
            }
            startActivity(intent)
        }

        addListingIcon.setOnClickListener {
            JobOptionsDialogFragment().show(supportFragmentManager, "JobOptionsDialogFragment")
        }

        // FIX 6: Set click handler on the FrameLayout container
        notificationIconContainer.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        backButtonIcon.setOnClickListener {
            viewPager.currentItem = 0
        }

        setupNotificationBadge() // Call the updated badge setup
    }

    // ... (checkNotificationPermission remains the same) ...


    private fun updateHeaderForPosition(position: Int) {
        val pageTitles = arrayOf("Home", "Rentals", "Post Ad", "Messages", "My Account")
        headerTitleTextView.text = pageTitles[position]

        val isHomeOrRentals = position == 0 || position == 1
        headerTabLayout.visibility = if (isHomeOrRentals) View.VISIBLE else View.GONE
        searchIcon.visibility = if (isHomeOrRentals) View.VISIBLE else View.GONE
        addListingIcon.visibility = if (position == 0) View.VISIBLE else View.GONE
        // FIX 7: Use the FrameLayout container for visibility logic
        notificationIconContainer.visibility = if(position == 0) View.VISIBLE else View.GONE
        backButtonIcon.visibility = if (!isHomeOrRentals) View.VISIBLE else View.GONE
    }


    fun openChatActivity(ad: Ad) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("AD_EXTRA", ad)
        }
        startActivity(intent)
    }

    /**
     * FIX: Updated function to observe the total notification count (Messages + Reviews)
     * and update the header badge TextView.
     */
    private fun setupNotificationBadge() {
        // We ensure the bottom navigation badge for messages is removed/updated
        // if it was previously displaying the total count.
        bottomNavigationView.removeBadge(R.id.nav_messages)

        sharedViewModel.totalNotificationCount.observe(this) { count ->
            if (count != null && count > 0) {
                // Set text on the header badge, limiting the display to "99+"
                notificationBadgeTextView.text = if (count > 99) "99+" else count.toString()
                notificationBadgeTextView.visibility = View.VISIBLE
            } else {
                notificationBadgeTextView.visibility = View.GONE
            }

            // Re-apply the bottom navigation message badge for messages ONLY
            // This ensures messages still show up on the "Messages" tab.
            val messageCount = sharedViewModel.unreadMessageCount.value ?: 0
            if (messageCount > 0) {
                val messageBadge: BadgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.nav_messages)
                messageBadge.number = messageCount
                messageBadge.isVisible = true
            } else {
                bottomNavigationView.removeBadge(R.id.nav_messages)
            }
        }
    }

    // FIX: New function to fetch initial total notification count (Messages + Reviews) from API
    private fun fetchInitialNotificationCount() {
        val token = sessionManager.fetchAuthToken()
        if (token == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // This call uses the updated API service function
                val response = ApiClient().getApiService(this@MainAppActivity).getNotificationCounts("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val counts = response.body()!!
                    // Update the SharedViewModel LiveData with the fetched data
                    sharedViewModel.updateTotalNotificationCount(counts.unreadMessageCount, counts.unreadReviewCount)
                    Log.d("NotificationBadge", "Initial counts: Messages=${counts.unreadMessageCount}, Reviews=${counts.unreadReviewCount}")
                } else {
                    Log.e("NotificationBadge", "Failed to fetch initial notification counts: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NotificationBadge", "Error fetching initial notification counts: ${e.message}")
            }
        }
    }

    private fun retrieveAndSendFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "Current token: $token")
            sendTokenToServer(token)
        }
    }

    private fun sendTokenToServer(token: String) {
        if (sessionManager.fetchAuthToken() == null) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiClient = ApiClient()
                val response = apiClient.getApiService(this@MainAppActivity).updateFcmToken(FcmTokenRequest(token))

                if (response.isSuccessful) {
                    Log.d("FCM", "Token updated successfully on server from MainAppActivity.")
                } else {
                    Log.e("FCM", "Failed to update token on server from MainAppActivity: ${response.errorBody()?.string()}")
                }
            } catch (e: IOException) {
                Log.e("FCM", "Network error while sending token from MainAppActivity: ${e.message}")
            } catch (e: HttpException) {
                Log.e("FCM", "HTTP error while sending token from MainAppActivity: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.disconnect()
    }
}