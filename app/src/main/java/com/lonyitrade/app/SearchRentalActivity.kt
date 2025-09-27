package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.RentalAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Rental
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchRentalActivity : AppCompatActivity() {

    // --- UI Elements ---
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var noResultsTextView: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var searchFormLayout: LinearLayout
    private lateinit var collapsedSearchIcon: ImageView
    private lateinit var searchButton: Button
    private lateinit var backButton: ImageView

    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(this) }

    // Form Fields
    private lateinit var propertyTypeSpinner: Spinner
    private lateinit var districtEditText: EditText
    private lateinit var minRoomsEditText: EditText
    private lateinit var minRentEditText: EditText
    private lateinit var maxRentEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_rental)

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
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        noResultsTextView = findViewById(R.id.noResultsTextView)
        searchFormLayout = findViewById(R.id.searchFormLayout)
        collapsedSearchIcon = findViewById(R.id.collapsedSearchIcon)
        searchButton = findViewById(R.id.search_button)
        backButton = findViewById(R.id.backButton)

        // Initialize form fields
        propertyTypeSpinner = findViewById(R.id.propertyTypeSpinner)
        districtEditText = findViewById(R.id.search_district)
        minRoomsEditText = findViewById(R.id.search_min_rooms)
        minRentEditText = findViewById(R.id.search_min_rent)
        maxRentEditText = findViewById(R.id.search_max_rent)
    }

    private fun setupRecyclerView() {
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun performSearch() {
        val propertyType = if (propertyTypeSpinner.selectedItemPosition == 0) null else propertyTypeSpinner.selectedItem.toString()
        val district = districtEditText.text.toString()
        val minRooms = minRoomsEditText.text.toString()
        val minRent = minRentEditText.text.toString()
        val maxRent = maxRentEditText.text.toString()

        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Correctly use the apiService instance
                val response = apiService.getRentals()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        var rentals = response.body() ?: emptyList()

                        // --- Client-Side Filtering (for demonstration) ---
                        if (!district.isNullOrEmpty()) {
                            rentals = rentals.filter { it.district.equals(district, ignoreCase = true) }
                        }
                        if (!minRooms.isNullOrEmpty()) {
                            rentals = rentals.filter { it.rooms != null && it.rooms >= minRooms.toInt() }
                        }
                        if (propertyType != null) {
                            rentals = rentals.filter { it.property_type.equals(propertyType, ignoreCase = true) }
                        }
                        if (!minRent.isNullOrEmpty()) {
                            rentals = rentals.filter { it.monthly_rent != null && it.monthly_rent >= minRent.toDouble() }
                        }
                        if (!maxRent.isNullOrEmpty()) {
                            rentals = rentals.filter { it.monthly_rent != null && it.monthly_rent <= maxRent.toDouble() }
                        }


                        if (rentals.isEmpty()) {
                            showNoResults()
                        } else {
                            showRentalResults(rentals)
                        }
                    } else {
                        showNoResults()
                        Toast.makeText(this@SearchRentalActivity, "Failed to get results: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showNoResults()
                    Toast.makeText(this@SearchRentalActivity, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
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

    private fun showRentalResults(rentals: List<Rental>) {
        searchResultsRecyclerView.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE
        val rentalAdapter = RentalAdapter(rentals, sessionManager.getUserId())
        searchResultsRecyclerView.adapter = rentalAdapter
    }

    private fun showNoResults() {
        searchResultsRecyclerView.visibility = View.GONE
        noResultsTextView.visibility = View.VISIBLE
    }
}