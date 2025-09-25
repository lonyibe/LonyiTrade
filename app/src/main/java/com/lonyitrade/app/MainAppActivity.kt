// File: lonyibe/lonyitrade/LonyiTrade-d16f52da8b320ce4e7eb82e1615d8393eafe26fa/app/src/main/java/com/lonyitrade/app/MainAppActivity.kt

package com.lonyitrade.app

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.lonyitrade.app.adapters.MainAppPagerAdapter
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.NetworkChangeReceiver
import com.lonyitrade.app.utils.SessionManager
import com.lonyitrade.app.utils.WebSocketManager
import com.lonyitrade.app.viewmodels.SharedViewModel

class MainAppActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var headerTabLayout: TabLayout
    private lateinit var searchIcon: ImageView
    private lateinit var addListingIcon: ImageView
    private lateinit var headerTitleTextView: TextView
    private lateinit var backButtonIcon: ImageView
    private lateinit var sessionManager: SessionManager
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        sessionManager = SessionManager(this)

        viewPager = findViewById(R.id.main_view_pager)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        headerTabLayout = findViewById(R.id.header_tab_layout)
        searchIcon = findViewById(R.id.search_icon)
        addListingIcon = findViewById(R.id.add_listing_icon)
        headerTitleTextView = findViewById(R.id.header_title_text_view)
        backButtonIcon = findViewById(R.id.back_button_icon)

        // Fix: Removed 'no_internet_view' which is not in this layout. Fragments handle their own network error views.

        sessionManager.fetchAuthToken()?.let { token ->
            // Fix: Changed getInstance() to directly access the object and its connect method.
            WebSocketManager.connect(token)
            // Fix: Pass the sharedViewModel to the WebSocketManager for message count updates.
            WebSocketManager.setSharedViewModel(sharedViewModel)
        }

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
                    2 -> "trending"
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

        backButtonIcon.setOnClickListener {
            viewPager.currentItem = 0
        }

        // Fix: Removed the unnecessary NetworkChangeReceiver instantiation and noInternetView since fragments handle their own network UI.

        setupMessageBadge()
    }

    private fun updateHeaderForPosition(position: Int) {
        val pageTitles = arrayOf("Home", "Rentals", "Post Ad", "Messages", "My Account")
        headerTitleTextView.text = pageTitles[position]

        val isHomeOrRentals = position == 0 || position == 1
        headerTabLayout.visibility = if (isHomeOrRentals) View.VISIBLE else View.GONE
        searchIcon.visibility = if (isHomeOrRentals) View.VISIBLE else View.GONE
        addListingIcon.visibility = if (position == 0) View.VISIBLE else View.GONE
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

    override fun onResume() {
        super.onResume()
        // Fix: Register receiver only if it is initialized, which is not the case here. Let the fragments handle it.
    }

    override fun onPause() {
        super.onPause()
        // Fix: Unregister receiver only if it is initialized.
    }


    override fun onDestroy() {
        super.onDestroy()
        // Fix: Directly call the disconnect method on the singleton object.
        WebSocketManager.disconnect()
    }
}