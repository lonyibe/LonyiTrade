package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class SearchActivity : AppCompatActivity() {

    // --- UI Elements ---
    private lateinit var listingTypeCard: MaterialCardView
    private lateinit var adSearchOptionsCard: MaterialCardView
    private lateinit var searchTitle: TextView

    private var currentSearchType: String = "ads"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Determine if this is a search for 'ads' or 'rentals' from the intent
        currentSearchType = intent.getStringExtra("searchType") ?: "ads"

        initializeViews()
        configureUiForSearchType()

        val searchButton = findViewById<Button>(R.id.search_button)
        searchButton.setOnClickListener {
            // This is a placeholder for your search logic.
            Toast.makeText(this, "Searching for $currentSearchType...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews() {
        listingTypeCard = findViewById(R.id.listingTypeCard)
        adSearchOptionsCard = findViewById(R.id.adSearchOptionsCard)
        searchTitle = findViewById(R.id.search_title)
    }

    private fun configureUiForSearchType() {
        if (currentSearchType == "rentals") {
            // This is a rental search.
            searchTitle.text = "Search Rentals"
            // Hide the Ad-specific UI elements.
            listingTypeCard.visibility = View.GONE
            adSearchOptionsCard.visibility = View.GONE
        } else {
            // This is an ad search.
            searchTitle.text = "Search Ads"
            // Make sure the Ad-specific UI elements are visible.
            listingTypeCard.visibility = View.VISIBLE
            adSearchOptionsCard.visibility = View.VISIBLE
        }
    }
}