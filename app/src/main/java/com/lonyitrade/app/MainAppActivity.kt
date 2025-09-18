package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lonyitrade.app.adapters.MainAppPagerAdapter // Added this import
import com.lonyitrade.app.data.models.Ad

class MainAppActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        viewPager = findViewById(R.id.main_view_pager)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        val searchIcon = findViewById<ImageView>(R.id.search_icon)

        // Set up the adapter for ViewPager2
        viewPager.adapter = MainAppPagerAdapter(this)

        // Set up swipe navigation
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavigationView.menu.getItem(position).isChecked = true
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

        // Set the default page
        if (savedInstanceState == null) {
            viewPager.currentItem = 0
        }

        searchIcon.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
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