package com.lonyitrade.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lonyitrade.app.adapters.AdAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.api.ApiService
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.NetworkUtils
import com.lonyitrade.app.utils.SessionManager
import com.lonyitrade.app.viewmodels.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adsRecyclerView: RecyclerView
    private lateinit var noAdsTextView: TextView
    private lateinit var adAdapter: AdAdapter
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var categoryContainer: LinearLayout
    private lateinit var networkErrorLayout: LinearLayout // NEW: Reference for the network error layout
    private var selectedCategory: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectedCategory != null) {
                    fetchAllAdverts(null)
                    selectedCategory = null
                    Toast.makeText(requireContext(), "Showing all ads", Toast.LENGTH_SHORT).show()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        adsRecyclerView = view.findViewById(R.id.adsRecyclerView)
        noAdsTextView = view.findViewById(R.id.noAdsTextView)
        categoryContainer = view.findViewById(R.id.categoryContainer)
        networkErrorLayout = view.findViewById(R.id.networkErrorLayout) // NEW: Initialize the network error layout
        adsRecyclerView.layoutManager = LinearLayoutManager(context)

        setupCategoryBubbles()

        sharedViewModel.adList.observe(viewLifecycleOwner) { updatedList ->
            val currentUserId = sessionManager.fetchAuthToken()
            adAdapter = AdAdapter(updatedList, currentUserId) { ad ->
                (activity as? MainAppActivity)?.openChatActivity(ad)
            }
            adsRecyclerView.adapter = adAdapter

            if (updatedList.isEmpty()) {
                adsRecyclerView.visibility = View.GONE
                noAdsTextView.visibility = View.VISIBLE
            } else {
                adsRecyclerView.visibility = View.VISIBLE
                noAdsTextView.visibility = View.GONE
            }
        }

        // Set up the refresh listener
        swipeRefreshLayout.setOnRefreshListener {
            fetchAllAdverts()
        }

        // Initial load of all adverts
        fetchAllAdverts()

        // FIX: Prevent ViewPager2 from intercepting horizontal scrolls on the category bar
        view.findViewById<HorizontalScrollView>(R.id.categoryScrollView).setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            v.onTouchEvent(event)
        }
    }

    private fun setupCategoryBubbles() {
        val categories = resources.getStringArray(R.array.categories)

        // Clear existing views to prevent duplicates
        categoryContainer.removeAllViews()

        for (category in categories) {
            val categoryView = LayoutInflater.from(context).inflate(R.layout.category_bubble, categoryContainer, false)
            val categoryTextView = categoryView.findViewById<TextView>(R.id.categoryTextView)
            categoryTextView.text = category
            categoryView.setOnClickListener {
                // When a category is clicked, fetch ads for that specific category
                selectedCategory = category
                fetchAllAdverts(category)
                Toast.makeText(context, "Searching for ads in $category...", Toast.LENGTH_SHORT).show()
            }
            categoryContainer.addView(categoryView)
        }
    }

    fun fetchAllAdverts(category: String? = null) {
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
                val response = if (category != null) {
                    ApiClient.apiService.searchAdverts(query = null, category = category, district = null, minPrice = null, maxPrice = null, type = "for_sale")
                } else {
                    ApiClient.apiService.getAdverts()
                }

                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false
                    if (response.isSuccessful) {
                        val fetchedAds = response.body()
                        sharedViewModel.setAdList(fetchedAds?.toMutableList() ?: mutableListOf())
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch ads: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                    showNetworkError()
                }
            }
        }
    }

    private fun showNetworkError() {
        // Hide regular content and show the network error layout
        adsRecyclerView.visibility = View.GONE
        noAdsTextView.visibility = View.GONE
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