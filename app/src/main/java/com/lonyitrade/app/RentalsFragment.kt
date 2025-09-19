package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lonyitrade.app.adapters.RentalAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.utils.NetworkUtils
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RentalsFragment : Fragment() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rentalsRecyclerView: RecyclerView
    private lateinit var noRentalsTextView: TextView
    private lateinit var rentalAdapter: RentalAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var networkErrorLayout: LinearLayout // NEW: Reference for the network error layout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rentals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        rentalsRecyclerView = view.findViewById(R.id.rentalsRecyclerView)
        noRentalsTextView = view.findViewById(R.id.noRentalsTextView)
        networkErrorLayout = view.findViewById(R.id.networkErrorLayout) // NEW: Initialize the network error layout
        rentalsRecyclerView.layoutManager = LinearLayoutManager(context)

        // Set up the refresh listener
        swipeRefreshLayout.setOnRefreshListener {
            fetchRentals()
        }

        fetchRentals()
    }

    private fun fetchRentals() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            showNetworkError()
            return
        }

        hideNetworkError()

        // Show the refresh spinner
        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = true
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getRentals()
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false // Stop the spinner
                    if (response.isSuccessful) {
                        val rentals = response.body() ?: emptyList()
                        val currentUserId = sessionManager.fetchAuthToken()
                        rentalAdapter = RentalAdapter(rentals, currentUserId)
                        rentalsRecyclerView.adapter = rentalAdapter

                        // Check if the list is empty
                        if (rentals.isEmpty()) {
                            rentalsRecyclerView.visibility = View.GONE
                            noRentalsTextView.visibility = View.VISIBLE
                        } else {
                            rentalsRecyclerView.visibility = View.VISIBLE
                            noRentalsTextView.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to load rentals", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false // Stop the spinner
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    showNetworkError()
                }
            }
        }
    }

    private fun showNetworkError() {
        // Hide regular content and show the network error layout
        rentalsRecyclerView.visibility = View.GONE
        noRentalsTextView.visibility = View.GONE
        networkErrorLayout.visibility = View.VISIBLE
        // Disable swipe refresh so the user cannot try to refresh while offline
        swipeRefreshLayout.isEnabled = false
    }

    private fun hideNetworkError() {
        // Hide the network error layout and show regular content views
        networkErrorLayout.visibility = View.GONE
        // Enable swipe refresh again
        swipeRefreshLayout.isEnabled = true
    }
}