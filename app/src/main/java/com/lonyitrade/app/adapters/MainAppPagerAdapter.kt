// File: C:/Users/Lonyibe/Desktop/LonyiTrade/app/src/main/java/com/lonyitrade/app/adapters/MainAppPagerAdapter.kt

package com.lonyitrade.app.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.lonyitrade.app.HomeFragment
import com.lonyitrade.app.MessagesFragment
import com.lonyitrade.app.MyAccountFragment
import com.lonyitrade.app.PostAdFragment
import com.lonyitrade.app.RentalsFragment

class MainAppPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 5 // Total number of menu items

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> RentalsFragment()
            2 -> PostAdFragment()
            3 -> MessagesFragment()
            4 -> MyAccountFragment()
            else -> throw IllegalStateException("Invalid position")
        }
    }
}