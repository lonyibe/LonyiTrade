package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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

        searchButton.setOnClickListener {
            val query = searchQuery.text.toString()
            val district = searchDistrict.text.toString()
            val minPrice = searchMinPrice.text.toString()
            val maxPrice = searchMaxPrice.text.toString()

            performSearch(query, district, minPrice, maxPrice)
        }
    }

    private fun performSearch(query: String, district: String, minPrice: String, maxPrice: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Your backend expects these query parameter names
                val response = ApiClient.apiService.searchAdverts(query, district, minPrice, maxPrice)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            sharedViewModel.setAdList(it.toMutableList())
                        }
                        dismiss() // Close the dialog
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}