package com.lonyitrade.app

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lonyitrade.app.adapters.MyListingsPagerAdapter

class MyAdsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ads)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val backButton = findViewById<ImageView>(R.id.backButton) // NEW: Find the back button

        // Set up the adapter for the ViewPager
        viewPager.adapter = MyListingsPagerAdapter(this)

        // Connect the TabLayout with the ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "My Ads"
                1 -> "My Rentals"
                else -> null
            }
        }.attach()

        // NEW: Set a click listener to the back button
        backButton.setOnClickListener {
            finish() // This closes the current activity and returns to the previous one
        }
    }
}