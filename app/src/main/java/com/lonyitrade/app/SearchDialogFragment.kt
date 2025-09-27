package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.viewmodels.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchDialogFragment : DialogFragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(requireContext()) }

    // New UI elements
    private lateinit var listingTypeRadioGroup: RadioGroup
    private lateinit var adSearchOptions: View
    private lateinit var adSearchTypeRadioGroup: RadioGroup
    private lateinit var adCategorySpinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchQuery = view.findViewById<EditText>(R.id.search_query)
        val searchDistrict = view.findViewById<EditText>(R.id.search_district)
        val searchMinPrice = view.findViewById<EditText>(R.id.search_min_price)
        val searchMaxPrice = view.findViewById<EditText>(R.id.search_max_price)
        val searchButton = view.findViewById<Button>(R.id.search_button)

        listingTypeRadioGroup = view.findViewById(R.id.listingTypeRadioGroup)
        adSearchOptions = view.findViewById(R.id.adSearchOptions)
        adSearchTypeRadioGroup = view.findViewById(R.id.adSearchTypeRadioGroup)
        adCategorySpinner = view.findViewById(R.id.adCategorySpinner)

        setupCategorySpinner()

        // Set up the listener for the listing type radio group
        listingTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.listingTypeAd -> adSearchOptions.visibility = View.VISIBLE
                R.id.listingTypeRental -> adSearchOptions.visibility = View.GONE
            }
        }

        searchButton.setOnClickListener {
            val query = searchQuery.text.toString()
            val district = searchDistrict.text.toString()
            val minPrice = searchMinPrice.text.toString()
            val maxPrice = searchMaxPrice.text.toString()

            val selectedListingType = listingTypeRadioGroup.checkedRadioButtonId
            val selectedAdType = adSearchTypeRadioGroup.checkedRadioButtonId

            performSearch(query, district, minPrice, maxPrice, selectedListingType, selectedAdType)
        }
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.categories)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adCategorySpinner.adapter = adapter
    }

    private fun performSearch(query: String, district: String, minPrice: String, maxPrice: String, listingType: Int, adType: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (listingType == R.id.listingTypeAd) {
                    val type = if (adType == R.id.adSearchBuy) "for_sale" else "wanted"
                    val category = adCategorySpinner.selectedItem?.toString()
                    // Correctly use the apiService instance
                    val response = apiService.searchAdverts(query, district, minPrice, maxPrice, type, category)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val ads = response.body()
                            if (ads.isNullOrEmpty()) {
                                // Inform the user that no ads were found
                                Toast.makeText(context, "No ads found matching your criteria.", Toast.LENGTH_LONG).show()
                                sharedViewModel.setAdList(mutableListOf())
                            } else {
                                sharedViewModel.setAdList(ads.toMutableList())
                            }
                            dismiss() // Close the dialog
                        } else {
                            Toast.makeText(context, "Failed to search for ads", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (listingType == R.id.listingTypeRental) {
                    // Correctly use the apiService instance
                    val response = apiService.getRentals()
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            // Since this returns Rentals, you would need to handle this differently,
                            // for example by navigating to the RentalsFragment.
                            Toast.makeText(context, "Rental search logic to be implemented", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}