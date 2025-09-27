// File: lonyibe/lonyitrade/LonyiTrade-79a0389294bf821249d6a729e75adea628882689/app/src/main/java/com/lonyitrade/app/MainAppActivity.kt

package com.lonyitrade.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
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
    private lateinit var notificationIcon: ImageView
    private lateinit var headerTitleTextView: TextView
    private lateinit var backButtonIcon: ImageView
    private lateinit var sessionManager: SessionManager
    private val sharedViewModel: SharedViewModel by viewModels()

    // --- FIX: Activity Result Launcher for Notification Permission (Android 13+) ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM", "POST_NOTIFICATIONS permission granted. Notifications enabled.")
        } else {
            Log.w("FCM", "POST_NOTIFICATIONS permission denied. Notifications may be disabled by the system.")
            Toast.makeText(this, "Notification permission denied. You may not receive updates.", Toast.LENGTH_LONG).show()
        }
    }
    // -------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        sessionManager = SessionManager(this)

        viewPager = findViewById(R.id.main_view_pager)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        headerTabLayout = findViewById(R.id.header_tab_layout)
        searchIcon = findViewById(R.id.search_icon)
        addListingIcon = findViewById(R.id.add_listing_icon)
        notificationIcon = findViewById(R.id.notification_icon)
        headerTitleTextView = findViewById(R.id.header_title_text_view)
        backButtonIcon = findViewById(R.id.back_button_icon)

        // Pass the sharedViewModel to the WebSocketManager first
        WebSocketManager.setSharedViewModel(sharedViewModel)
        // Then connect with the token
        sessionManager.fetchAuthToken()?.let { token ->
            WebSocketManager.connect(token)
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
                if (currentFragment is HomeFragment) {
                    currentFragment.resetToHomePage()
                }
            }
        }

        headerTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val currentFragment = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
                val sortBy = when (tab?.position) {
                    0 -> "top"
                    1 -> "latest"
                    else -> "latest"
                }

                if (currentFragment is HomeFragment) {
                    currentFragment.fetchAllAdverts(sortBy = sortBy)
                } else if (currentFragment is RentalsFragment) {
                    currentFragment.fetchRentals(sortBy = sortBy)
                }
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

        notificationIcon.setOnClickListener {
            // Handle your notification click logic here
            Toast.makeText(this, "Notification icon clicked!", Toast.LENGTH_SHORT).show()
        }

        backButtonIcon.setOnClickListener {
            viewPager.currentItem = 0
        }

        setupMessageBadge()
    }

    // --- FIX: Function to check and request the notification permission ---
    private fun checkNotificationPermission() {
        // Permission check is only required for Android 13 (Tiramisu, API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // -----------------------------------------------------------------------


    private fun updateHeaderForPosition(position: Int) {
        val pageTitles = arrayOf("Home", "Rentals", "Post Ad", "Messages", "My Account")
        headerTitleTextView.text = pageTitles[position]

        val isHomeOrRentals = position == 0 || position == 1
        headerTabLayout.visibility = if (isHomeOrRentals) View.VISIBLE else View.GONE
        searchIcon.visibility = if (isHomeOrRentals) View.VISIBLE else View.GONE
        addListingIcon.visibility = if (position == 0) View.VISIBLE else View.GONE
        notificationIcon.visibility = if(position == 0) View.VISIBLE else View.GONE
        backButtonIcon.visibility = if (!isHomeOrRentals) View.VISIBLE else View.GONE
    }


    fun openChatActivity(ad: Ad) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("AD_EXTRA", ad)
        }
        startActivity(intent)
    }

    private fun setupMessageBadge() {
        val badge: BadgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.nav_messages)
        badge.isVisible = false // Initially hidden

        sharedViewModel.unreadMessageCount.observe(this) { count ->
            if (count != null && count > 0) {
                badge.number = count
                badge.isVisible = true
            } else {
                badge.isVisible = false
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