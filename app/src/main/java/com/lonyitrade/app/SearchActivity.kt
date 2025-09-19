package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.lonyitrade.app.adapters.AdAdapter
import com.lonyitrade.app.adapters.RentalAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.Rental
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    // --- UI Elements ---
    private lateinit var listingTypeCard: MaterialCardView
    private lateinit var adSearchOptionsCard: MaterialCardView
    private lateinit var searchTitle: TextView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var noResultsTextView: TextView
    private lateinit var sessionManager: SessionManager

    private var currentSearchType: String = "ads"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        sessionManager = SessionManager(this)

        // Determine if this is a search for 'ads' or 'rentals' from the intent
        currentSearchType = intent.getStringExtra("searchType") ?: "ads"

        initializeViews()
        configureUiForSearchType()
        setupRecyclerView()

        val searchButton = findViewById<Button>(R.id.search_button)
        searchButton.setOnClickListener {
            performSearch()
        }
    }

    private fun initializeViews() {
        listingTypeCard = findViewById(R.id.listingTypeCard)
        adSearchOptionsCard = findViewById(R.id.adSearchOptionsCard)
        searchTitle = findViewById(R.id.search_title)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        noResultsTextView = findViewById(R.id.noResultsTextView)
    }

    private fun setupRecyclerView() {
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
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

    private fun performSearch() {
        val query = findViewById<EditText>(R.id.search_query).text.toString()
        val district = findViewById<EditText>(R.id.search_district).text.toString()
        val minPrice = findViewById<EditText>(R.id.search_min_price).text.toString()
        val maxPrice = findViewById<EditText>(R.id.search_max_price).text.toString()

        Toast.makeText(this, "Searching for $currentSearchType...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (currentSearchType == "ads") {
                    val response = ApiClient.apiService.searchAdverts(query, district, minPrice, maxPrice, null, null)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val ads = response.body()
                            if (ads.isNullOrEmpty()) {
                                showNoResults()
                            } else {
                                showAdResults(ads)
                            }
                        } else {
                            showNoResults()
                        }
                    }
                } else { // Rentals
                    // Note: The rentals search in ApiService is not yet implemented with all parameters
                    val response = ApiClient.apiService.getRentals() // Simplified for now
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val rentals = response.body()
                            if (rentals.isNullOrEmpty()) {
                                showNoResults()
                            } else {
                                showRentalResults(rentals)
                            }
                        } else {
                            showNoResults()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showNoResults()
                    Toast.makeText(this@SearchActivity, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showAdResults(ads: List<Ad>) {
        searchResultsRecyclerView.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE
        val adAdapter = AdAdapter(ads, sessionManager.fetchAuthToken()) { /* Handle message click if needed */ }
        searchResultsRecyclerView.adapter = adAdapter
    }

    private fun showRentalResults(rentals: List<Rental>) {
        searchResultsRecyclerView.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE
        val rentalAdapter = RentalAdapter(rentals, sessionManager.fetchAuthToken())
        searchResultsRecyclerView.adapter = rentalAdapter
    }

    private fun showNoResults() {
        searchResultsRecyclerView.visibility = View.GONE
        noResultsTextView.visibility = View.VISIBLE
    }
}