package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lonyitrade.app.adapters.RentalAdapter
import com.lonyitrade.app.api.ApiClient
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
        rentalsRecyclerView.layoutManager = LinearLayoutManager(context)

        // Set up the refresh listener
        swipeRefreshLayout.setOnRefreshListener {
            fetchRentals()
        }

        fetchRentals()
    }

    private fun fetchRentals() {
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
                }
            }
        }
    }
}