package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lonyitrade.app.adapters.AdPhotoAdapter
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.SessionManager

class AdDetailActivity : AppCompatActivity() {

    private lateinit var ad: Ad
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad_detail)

        sessionManager = SessionManager(this)
        ad = intent.getParcelableExtra("AD_EXTRA") ?: run {
            Toast.makeText(this, "Error loading ad data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
    }

    private fun initializeViews() {
        // Toolbar
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
        findViewById<TextView>(R.id.adTitleTextView).text = ad.title

        // Ad Details
        val adViewPager: ViewPager2 = findViewById(R.id.adPhotosViewPager)
        val tabLayout: TabLayout = findViewById(R.id.photoTabLayout)
        val categoryTextView: TextView = findViewById(R.id.adCategoryTextView)
        val descriptionTextView: TextView = findViewById(R.id.adDescriptionTextView)
        val priceTextView: TextView = findViewById(R.id.adPriceTextView)
        val priceTypeTextView: TextView = findViewById(R.id.adPriceTypeTextView)
        val conditionTextView: TextView = findViewById(R.id.adConditionTextView)
        val locationTextView: TextView = findViewById(R.id.adLocationTextView)
        val phoneNumberTextView: TextView = findViewById(R.id.adPhoneNumberTextView)
        val adTypeTextView: TextView = findViewById(R.id.adTypeTextView)
        val messageSellerButton: Button = findViewById(R.id.messageSellerButton)
        val reviewButton: Button = findViewById(R.id.reviewButton)

        // Set Ad Details
        categoryTextView.text = "Category: ${ad.category}"
        descriptionTextView.text = ad.description
        priceTextView.text = "UGX ${ad.price ?: "0"}"
        priceTypeTextView.text = ad.priceType ?: ""
        conditionTextView.text = "Condition: ${ad.condition ?: "N/A"}"
        locationTextView.text = ad.district ?: "N/A"
        phoneNumberTextView.text = ad.sellerPhoneNumber ?: "N/A"

        // Handle Ad Type Badge
        when (ad.type) {
            "for_sale" -> {
                adTypeTextView.text = "For Sale"
                adTypeTextView.setBackgroundColor(ContextCompat.getColor(this, R.color.status_success_green))
            }
            "wanted" -> {
                adTypeTextView.text = "Wanted"
                adTypeTextView.setBackgroundColor(ContextCompat.getColor(this, R.color.status_error_red))
                priceTextView.text = "Budget: UGX ${ad.price ?: "0"}"
            }
            else -> adTypeTextView.visibility = View.GONE
        }

        // Handle Photos
        val photos = ad.photos
        if (!photos.isNullOrEmpty()) {
            val photoAdapter = AdPhotoAdapter(photos)
            adViewPager.adapter = photoAdapter
            TabLayoutMediator(tabLayout, adViewPager) { _, _ -> }.attach()
            tabLayout.visibility = View.VISIBLE

            // Add click listener to open full screen view
            photoAdapter.setOnItemClickListener(object : AdPhotoAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    val intent = Intent(this@AdDetailActivity, FullScreenImageActivity::class.java).apply {
                        putStringArrayListExtra("image_urls", ArrayList(photos))
                        putExtra("position", position)
                    }
                    startActivity(intent)
                }
            })
        } else {
            adViewPager.visibility = View.GONE
            tabLayout.visibility = View.GONE
        }

        // Handle "Message Seller" button visibility and action
        val currentUserId = sessionManager.getUserId()
        if (ad.userId == currentUserId) {
            messageSellerButton.visibility = View.GONE
        } else {
            messageSellerButton.visibility = View.VISIBLE
            messageSellerButton.setOnClickListener {
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("AD_EXTRA", ad)
                }
                startActivity(intent)
            }
        }

        reviewButton.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java)
            startActivity(intent)
        }
    }
}