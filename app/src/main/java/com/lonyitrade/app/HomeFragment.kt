package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lonyitrade.app.adapters.AdAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.api.ApiService
import com.lonyitrade.app.data.models.Ad
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
                fetchAllAdverts(category)
                Toast.makeText(context, "Searching for ads in $category...", Toast.LENGTH_SHORT).show()
            }
            categoryContainer.addView(categoryView)
        }
    }

    fun fetchAllAdverts(category: String? = null) {
        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = true
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Correctly pass the category to the 'category' parameter, not 'q'
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
                }
            }
        }
    }
}
