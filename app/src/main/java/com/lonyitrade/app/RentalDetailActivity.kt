package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lonyitrade.app.adapters.RentalPhotoAdapter
import com.lonyitrade.app.data.models.Rental

class RentalDetailActivity : AppCompatActivity() {

    private lateinit var rental: Rental

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rental_detail)

        rental = intent.getParcelableExtra("RENTAL_EXTRA") ?: run {
            Toast.makeText(this, "Error loading rental data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
    }

    private fun initializeViews() {
        // Toolbar
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
        findViewById<TextView>(R.id.rentalTitleTextView).text = rental.property_type ?: "Rental Details"

        // Rental Details
        val rentalViewPager: ViewPager2 = findViewById(R.id.rentalPhotosViewPager)
        val tabLayout: TabLayout = findViewById(R.id.rentalPhotoTabLayout)
        val rentalPriceTextView: TextView = findViewById(R.id.rentalPriceTextView)
        val rentalPriceTypeTextView: TextView = findViewById(R.id.rentalPriceTypeTextView)
        val rentalDescriptionTextView: TextView = findViewById(R.id.rentalDescriptionTextView)
        val rentalRoomsTextView: TextView = findViewById(R.id.rentalRoomsTextView)
        val rentalRulesTextView: TextView = findViewById(R.id.rentalRulesTextView)
        val rentalLocationTextView: TextView = findViewById(R.id.rentalLocationTextView)
        val landlordNameTextView: TextView = findViewById(R.id.landlordNameTextView)
        val landlordPhoneTextView: TextView = findViewById(R.id.landlordPhoneTextView)

        rentalPriceTextView.text = "UGX ${rental.monthly_rent ?: 0} / month"
        rentalPriceTypeTextView.text = "(${rental.price_type ?: "N/A"})"
        rentalDescriptionTextView.text = rental.description ?: "No description provided."
        rentalRoomsTextView.text = "${rental.rooms ?: 0} Rooms"
        rentalRulesTextView.text = rental.rules ?: "No rules specified."
        rentalLocationTextView.text = "${rental.city ?: "N/A"}, ${rental.district ?: "N/A"}"
        landlordNameTextView.text = "${rental.landlord_name ?: "N/A"} (${rental.landlord_type ?: "N/A"})"
        landlordPhoneTextView.text = rental.landlord_phone ?: "N/A"

        // Handle Photos
        if (!rental.photos.isNullOrEmpty()) {
            val photoAdapter = RentalPhotoAdapter(rental.photos!!)
            rentalViewPager.adapter = photoAdapter
            TabLayoutMediator(tabLayout, rentalViewPager) { tab, position -> }.attach()
            tabLayout.visibility = View.VISIBLE
        } else {
            rentalViewPager.visibility = View.GONE
            tabLayout.visibility = View.GONE
        }
    }
}