package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

// Placeholder fragments that are not yet fully built out
class MessagesFragment : Fragment(R.layout.fragment_messages)

class MainAppActivity : AppCompatActivity() {

    // 1. Create instances of all fragments, including the new RentalsFragment
    private val homeFragment = HomeFragment()
    private val rentalsFragment = RentalsFragment() // New
    private val postAdFragment = PostAdFragment()
    private val messagesFragment = MessagesFragment()
    private val myAccountFragment = MyAccountFragment()

    // 2. Keep track of the active fragment
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val searchIcon = findViewById<ImageView>(R.id.search_icon)

        // 3. Add all fragments to the manager
        supportFragmentManager.beginTransaction().apply {
            add(R.id.main_frame_layout, myAccountFragment, "5").hide(myAccountFragment)
            add(R.id.main_frame_layout, messagesFragment, "4").hide(messagesFragment)
            add(R.id.main_frame_layout, postAdFragment, "3").hide(postAdFragment)
            add(R.id.main_frame_layout, rentalsFragment, "2").hide(rentalsFragment) // New
            add(R.id.main_frame_layout, homeFragment, "1")
        }.commit()

        // 4. Update the listener to handle the new nav_rentals item
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    homeFragment.fetchAllAdverts()
                    showFragment(homeFragment)
                }
                R.id.nav_rentals -> showFragment(rentalsFragment) // New
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
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit()
        activeFragment = fragment
    }
}