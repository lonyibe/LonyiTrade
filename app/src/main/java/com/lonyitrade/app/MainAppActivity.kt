package com.lonyitrade.app

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

// We still need the placeholder for MessagesFragment until we build it out
class MessagesFragment : Fragment(R.layout.fragment_messages)
// MyAccountFragment placeholder has been REMOVED from here

class MainAppActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val postAdFragment = PostAdFragment()
    private val messagesFragment = MessagesFragment()
    private val myAccountFragment = MyAccountFragment() // This now correctly refers to your full MyAccountFragment.kt file
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val searchIcon = findViewById<ImageView>(R.id.search_icon)

        supportFragmentManager.beginTransaction().apply {
            add(R.id.main_frame_layout, myAccountFragment, "4").hide(myAccountFragment)
            add(R.id.main_frame_layout, messagesFragment, "3").hide(messagesFragment)
            add(R.id.main_frame_layout, postAdFragment, "2").hide(postAdFragment)
            add(R.id.main_frame_layout, homeFragment, "1")
        }.commit()

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    homeFragment.fetchAllAdverts()
                    showFragment(homeFragment)
                }
                R.id.nav_post_ad -> showFragment(postAdFragment)
                R.id.nav_messages -> showFragment(messagesFragment)
                R.id.nav_my_account -> showFragment(myAccountFragment)
            }
            true
        }

        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_home
        }

        searchIcon.setOnClickListener {
            SearchDialogFragment().show(supportFragmentManager, "SearchDialog")
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit()
        activeFragment = fragment
    }
}