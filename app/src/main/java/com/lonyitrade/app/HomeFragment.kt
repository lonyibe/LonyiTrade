package com.lonyitrade.app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
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
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.NetworkChangeReceiver
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
    private lateinit var networkErrorLayout: LinearLayout
    private var selectedCategory: String? = null
    private var currentSortBy: String = "latest" // Default sort

    private lateinit var networkChangeReceiver: NetworkChangeReceiver

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectedCategory != null) {
                    resetToHomePage()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        adsRecyclerView = view.findViewById(R.id.adsRecyclerView)
        noAdsTextView = view.findViewById(R.id.noAdsTextView)
        categoryContainer = view.findViewById(R.id.categoryContainer)
        networkErrorLayout = view.findViewById(R.id.networkErrorLayout)
        adsRecyclerView.layoutManager = LinearLayoutManager(context)

        networkChangeReceiver = NetworkChangeReceiver(
            onNetworkAvailable = { fetchAllAdverts() },
            onNetworkLost = { showNetworkError() }
        )

        setupCategoryBubbles()

        sharedViewModel.adList.observe(viewLifecycleOwner) { updatedList ->
            // FIX: Correctly get the userId from SessionManager
            val currentUserId = sessionManager.getUserId()
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

        swipeRefreshLayout.setOnRefreshListener {
            fetchAllAdverts(category = selectedCategory, sortBy = currentSortBy)
        }

        view.findViewById<HorizontalScrollView>(R.id.categoryScrollView).setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action) {
                MotionEvent.ACTION_UP -> v.parent.requestDisallowInterceptTouchEvent(false)
            }
            v.onTouchEvent(event)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchAllAdverts(sortBy = currentSortBy)
    }

    override fun onStart() {
        super.onStart()
        requireContext().registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(networkChangeReceiver)
    }

    private fun setupCategoryBubbles() {
        val categories = resources.getStringArray(R.array.categories)
        categoryContainer.removeAllViews()

        for (category in categories) {
            val categoryView = LayoutInflater.from(context).inflate(R.layout.category_bubble, categoryContainer, false)
            val categoryTextView = categoryView.findViewById<TextView>(R.id.categoryTextView)
            categoryTextView.text = category
            categoryView.setOnClickListener {
                selectedCategory = category
                fetchAllAdverts(category, currentSortBy)
                Toast.makeText(context, "Searching for ads in $category...", Toast.LENGTH_SHORT).show()
            }
            categoryContainer.addView(categoryView)
        }
    }

    fun fetchAllAdverts(category: String? = null, sortBy: String = "latest") {
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
                val response = if (category != null) {
                    ApiClient.apiService.searchAdverts(query = null, category = category, district = null, minPrice = null, maxPrice = null, type = "for_sale")
                } else {
                    ApiClient.apiService.getAdverts(sortBy)
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

    fun resetToHomePage() {
        if (selectedCategory != null) {
            selectedCategory = null
            fetchAllAdverts(sortBy = currentSortBy)
            Toast.makeText(requireContext(), "Showing all ads", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNetworkError() {
        adsRecyclerView.visibility = View.GONE
        noAdsTextView.visibility = View.GONE
        networkErrorLayout.visibility = View.VISIBLE
        swipeRefreshLayout.isEnabled = false
    }

    private fun hideNetworkError() {
        networkErrorLayout.visibility = View.GONE
        swipeRefreshLayout.isEnabled = true
    }
}