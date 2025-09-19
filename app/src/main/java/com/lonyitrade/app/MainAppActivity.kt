package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.lonyitrade.app.adapters.MainAppPagerAdapter
import com.lonyitrade.app.data.models.Ad

class MainAppActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var headerTabLayout: TabLayout
    private lateinit var searchIcon: ImageView // Reference for the search icon
    private lateinit var addListingIcon: ImageView // Reference for the add icon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        viewPager = findViewById(R.id.main_view_pager)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        headerTabLayout = findViewById(R.id.header_tab_layout)
        searchIcon = findViewById(R.id.search_icon)
        addListingIcon = findViewById(R.id.add_listing_icon)

        // Set up the adapter for ViewPager2
        viewPager.adapter = MainAppPagerAdapter(this)

        // This change will ensure a smoother transition between Fragments
        viewPager.offscreenPageLimit = 4

        // Set up swipe navigation and control header visibility
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavigationView.menu.getItem(position).isChecked = true

                // Show header elements only for the Home screen (position 0)
                if (position == 0) {
                    headerTabLayout.visibility = View.VISIBLE
                    searchIcon.visibility = View.VISIBLE
                    addListingIcon.visibility = View.VISIBLE
                } else {
                    headerTabLayout.visibility = View.GONE
                    searchIcon.visibility = View.GONE
                    addListingIcon.visibility = View.GONE
                }
            }
        })

        // Set up BottomNavigationView to change pages
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> viewPager.currentItem = 0
                R.id.nav_rentals -> viewPager.currentItem = 1
                R.id.nav_post_ad -> viewPager.currentItem = 2
                R.id.nav_messages -> viewPager.currentItem = 3
                R.id.nav_my_account -> viewPager.currentItem = 4
            }
            true
        }

        // Set the default page and ensure correct header visibility on startup
        if (savedInstanceState == null) {
            viewPager.currentItem = 0
            headerTabLayout.visibility = View.VISIBLE
            searchIcon.visibility = View.VISIBLE
            addListingIcon.visibility = View.VISIBLE
        }

        searchIcon.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        addListingIcon.setOnClickListener {
            val dialog = JobOptionsDialogFragment()
            dialog.show(supportFragmentManager, "JobOptionsDialogFragment")
        }
    }

    // Function to open the chat activity
    fun openChatActivity(ad: Ad) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("AD_EXTRA", ad)
        }
        startActivity(intent)
    }
}