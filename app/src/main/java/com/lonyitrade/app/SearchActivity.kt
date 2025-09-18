package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.viewmodels.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {
    private val sharedViewModel: SharedViewModel by viewModels()

    private lateinit var listingTypeRadioGroup: RadioGroup
    private lateinit var adSearchOptions: View
    private lateinit var adSearchTypeRadioGroup: RadioGroup
    private lateinit var searchQueryEditText: EditText
    private lateinit var searchDistrictEditText: EditText
    private lateinit var searchMinPriceEditText: EditText
    private lateinit var searchMaxPriceEditText: EditText
    private lateinit var searchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        listingTypeRadioGroup = findViewById(R.id.listingTypeRadioGroup)
        adSearchOptions = findViewById(R.id.adSearchOptions)
        adSearchTypeRadioGroup = findViewById(R.id.adSearchTypeRadioGroup)
        searchQueryEditText = findViewById(R.id.search_query)
        searchDistrictEditText = findViewById(R.id.search_district)
        searchMinPriceEditText = findViewById(R.id.search_min_price)
        searchMaxPriceEditText = findViewById(R.id.search_max_price)
        searchButton = findViewById(R.id.search_button)
    }

    private fun setupListeners() {
        listingTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.listingTypeAd -> adSearchOptions.visibility = View.VISIBLE
                R.id.listingTypeRental -> adSearchOptions.visibility = View.GONE
            }
        }

        searchButton.setOnClickListener {
            performSearch()
        }
    }

    private fun performSearch() {
        val query = searchQueryEditText.text.toString().ifEmpty { null }
        val district = searchDistrictEditText.text.toString().ifEmpty { null }
        val minPrice = searchMinPriceEditText.text.toString().ifEmpty { null }
        val maxPrice = searchMaxPriceEditText.text.toString().ifEmpty { null }

        val selectedListingType = listingTypeRadioGroup.checkedRadioButtonId
        val selectedAdType = adSearchTypeRadioGroup.checkedRadioButtonId

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (selectedListingType == R.id.listingTypeAd) {
                    val type = if (selectedAdType == R.id.adSearchBuy) "for_sale" else "wanted"
                    val response = ApiClient.apiService.searchAdverts(query, district, minPrice, maxPrice, type)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                sharedViewModel.setAdList(it.toMutableList())
                            }
                            Toast.makeText(this@SearchActivity, "Ad search successful!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@SearchActivity, "Failed to search for ads", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (selectedListingType == R.id.listingTypeRental) {
                    val response = ApiClient.apiService.getRentals()
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@SearchActivity, "Rental search logic to be implemented", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@SearchActivity, "Failed to search for rentals", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SearchActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}