package com.lonyitrade.app

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lonyitrade.app.adapters.RentalAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Rental
import com.lonyitrade.app.utils.NetworkChangeReceiver
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
    private lateinit var networkErrorLayout: LinearLayout
    private lateinit var categoryContainer: LinearLayout
    private var selectedCategory: String? = null
    private var currentSortBy: String = "latest" // Default sort

    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(requireContext()) }

    private lateinit var networkChangeReceiver: NetworkChangeReceiver

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
        networkErrorLayout = view.findViewById(R.id.networkErrorLayout)
        categoryContainer = view.findViewById(R.id.categoryContainer)
        rentalsRecyclerView.layoutManager = LinearLayoutManager(context)

        networkChangeReceiver = NetworkChangeReceiver(
            onNetworkAvailable = { fetchRentals() },
            onNetworkLost = { showNetworkError() }
        )

        setupCategoryBubbles()

        swipeRefreshLayout.setOnRefreshListener {
            fetchRentals(selectedCategory, currentSortBy)
        }

        view.findViewById<HorizontalScrollView>(R.id.categoryScrollView).setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action) {
                MotionEvent.ACTION_UP -> v.parent.requestDisallowInterceptTouchEvent(false)
            }
            v.onTouchEvent(event)
        }
    }

    override fun onStart() {
        super.onStart()
        requireContext().registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(networkChangeReceiver)
    }

    override fun onResume() {
        super.onResume()
        fetchRentals(sortBy = currentSortBy)
    }

    private fun setupCategoryBubbles() {
        val categories = arrayOf("House", "Apartment", "Studio", "Condo", "Duplex", "Hostel", "Other")
        categoryContainer.removeAllViews()

        for (category in categories) {
            val categoryView = LayoutInflater.from(context).inflate(R.layout.category_bubble, categoryContainer, false)
            val categoryTextView = categoryView.findViewById<TextView>(R.id.categoryTextView)
            categoryTextView.text = category
            categoryView.setOnClickListener {
                selectedCategory = category
                fetchRentals(category, currentSortBy)
                Toast.makeText(context, "Searching for rentals in $category...", Toast.LENGTH_SHORT).show()
            }
            categoryContainer.addView(categoryView)
        }
    }

    fun fetchRentals(category: String? = null, sortBy: String = "latest") {
        currentSortBy = sortBy
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            showNetworkError()
            return
        }

        hideNetworkError()

        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = true
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Correctly use the apiService instance
                val response = apiService.getRentals(sortBy)
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false
                    if (response.isSuccessful) {
                        var rentals = response.body() ?: emptyList()
                        if (category != null) {
                            rentals = rentals.filter { it.property_type.equals(category, ignoreCase = true) }
                        }

                        val currentUserId = sessionManager.getUserId()
                        rentalAdapter = RentalAdapter(rentals, currentUserId)
                        rentalsRecyclerView.adapter = rentalAdapter

                        if (rentals.isEmpty()) {
                            rentalsRecyclerView.visibility = View.GONE
                            noRentalsTextView.visibility = View.VISIBLE
                        } else {
                            rentalsRecyclerView.visibility = View.VISIBLE
                            noRentalsTextView.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to load rentals: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    showNetworkError()
                }
            }
        }
    }

    private fun showNetworkError() {
        rentalsRecyclerView.visibility = View.GONE
        noRentalsTextView.visibility = View.GONE
        networkErrorLayout.visibility = View.VISIBLE
        swipeRefreshLayout.isEnabled = false
    }

    private fun hideNetworkError() {
        networkErrorLayout.visibility = View.GONE
        swipeRefreshLayout.isEnabled = true
    }
}