package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.lonyitrade.app.adapters.AdAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    // --- UI Elements ---
    private lateinit var adSearchOptionsCard: MaterialCardView
    private lateinit var searchTitle: TextView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var noResultsTextView: TextView
    private lateinit var sessionManager: SessionManager

    // New UI elements for animation
    private lateinit var searchFormLayout: LinearLayout
    private lateinit var collapsedSearchIcon: ImageView
    private lateinit var searchButton: Button
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        sessionManager = SessionManager(this)

        initializeViews()
        setupRecyclerView()

        searchButton.setOnClickListener {
            performSearch()
            collapseSearchForm()
        }

        collapsedSearchIcon.setOnClickListener {
            expandSearchForm()
        }

        backButton.setOnClickListener {
            if (searchFormLayout.visibility == View.GONE) {
                expandSearchForm()
            } else {
                finish()
            }
        }

        // Handle the system back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searchFormLayout.visibility == View.GONE) {
                    expandSearchForm()
                } else {
                    finish()
                }
            }
        })
    }

    private fun initializeViews() {
        adSearchOptionsCard = findViewById(R.id.adSearchOptionsCard)
        searchTitle = findViewById(R.id.search_title)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        noResultsTextView = findViewById(R.id.noResultsTextView)

        // Animation views
        searchFormLayout = findViewById(R.id.searchFormLayout)
        collapsedSearchIcon = findViewById(R.id.collapsedSearchIcon)
        searchButton = findViewById(R.id.search_button)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupRecyclerView() {
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun performSearch() {
        val query = findViewById<EditText>(R.id.search_query).text.toString()
        val district = findViewById<EditText>(R.id.search_district).text.toString()
        val minPrice = findViewById<EditText>(R.id.search_min_price).text.toString()
        val maxPrice = findViewById<EditText>(R.id.search_max_price).text.toString()

        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showNoResults()
                    Toast.makeText(this@SearchActivity, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun collapseSearchForm() {
        searchFormLayout.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                searchFormLayout.visibility = View.GONE
                searchButton.visibility = View.GONE
                collapsedSearchIcon.visibility = View.VISIBLE
                backButton.visibility = View.VISIBLE
            }
            .start()
    }

    private fun expandSearchForm() {
        searchResultsRecyclerView.visibility = View.GONE
        noResultsTextView.visibility = View.GONE
        searchResultsRecyclerView.adapter = null

        collapsedSearchIcon.visibility = View.GONE
        backButton.visibility = View.GONE
        searchFormLayout.visibility = View.VISIBLE
        searchButton.visibility = View.VISIBLE
        searchFormLayout.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun showAdResults(ads: List<Ad>) {
        searchResultsRecyclerView.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE
        val adAdapter = AdAdapter(ads, sessionManager.fetchAuthToken()) { /* Handle message click */ }
        searchResultsRecyclerView.adapter = adAdapter
    }

    private fun showNoResults() {
        searchResultsRecyclerView.visibility = View.GONE
        noResultsTextView.visibility = View.VISIBLE
    }
}