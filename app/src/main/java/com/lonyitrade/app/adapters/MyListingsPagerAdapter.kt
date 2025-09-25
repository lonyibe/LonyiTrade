package com.lonyitrade.app.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.lonyitrade.app.MyAdsFragment
import com.lonyitrade.app.MyRentalsFragment

class MyListingsPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 2 // We have two tabs: "My Ads" and "My Rentals"
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MyAdsFragment()
            1 -> MyRentalsFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}