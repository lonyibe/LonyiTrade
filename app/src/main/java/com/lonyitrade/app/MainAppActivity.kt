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
    private lateinit var searchIcon: ImageView
    private lateinit var addListingIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        viewPager = findViewById(R.id.main_view_pager)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        headerTabLayout = findViewById(R.id.header_tab_layout)
        searchIcon = findViewById(R.id.search_icon)
        addListingIcon = findViewById(R.id.add_listing_icon)

        viewPager.adapter = MainAppPagerAdapter(this)
        viewPager.offscreenPageLimit = 4

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavigationView.menu.getItem(position).isChecked = true

                // Position 0: Home, Position 1: Rentals
                when (position) {
                    0 -> { // Home Screen
                        headerTabLayout.visibility = View.VISIBLE
                        searchIcon.visibility = View.VISIBLE
                        addListingIcon.visibility = View.VISIBLE
                    }
                    1 -> { // Rentals Screen
                        headerTabLayout.visibility = View.GONE
                        searchIcon.visibility = View.VISIBLE
                        addListingIcon.visibility = View.GONE
                    }
                    else -> { // All other screens
                        headerTabLayout.visibility = View.GONE
                        searchIcon.visibility = View.GONE
                        addListingIcon.visibility = View.GONE
                    }
                }
            }
        })

        bottomNavigationView.setOnItemSelectedListener { item ->
            viewPager.currentItem = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_rentals -> 1
                R.id.nav_post_ad -> 2
                R.id.nav_messages -> 3
                R.id.nav_my_account -> 4
                else -> 0
            }
            true
        }

        if (savedInstanceState == null) {
            viewPager.currentItem = 0
            headerTabLayout.visibility = View.VISIBLE
            searchIcon.visibility = View.VISIBLE
            addListingIcon.visibility = View.VISIBLE
        }

        searchIcon.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            // Pass the current screen type to the search activity
            if (viewPager.currentItem == 1) { // 1 is the position for RentalsFragment
                intent.putExtra("searchType", "rentals")
            } else {
                intent.putExtra("searchType", "ads")
            }
            startActivity(intent)
        }

        addListingIcon.setOnClickListener {
            val dialog = JobOptionsDialogFragment()
            dialog.show(supportFragmentManager, "JobOptionsDialogFragment")
        }
    }

    fun openChatActivity(ad: Ad) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("AD_EXTRA", ad)
        }
        startActivity(intent)
    }
}