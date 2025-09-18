package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lonyitrade.app.adapters.AdAdapter
import com.lonyitrade.app.api.ApiClient
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
        adsRecyclerView.layoutManager = LinearLayoutManager(context)

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
    }

    override fun onResume() {
        super.onResume()
        fetchAllAdverts()
    }

    fun fetchAllAdverts() {
        // Show the refresh spinner
        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = true
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getAdverts()
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false // Stop the spinner
                    if (response.isSuccessful) {
                        val fetchedAds = response.body()
                        sharedViewModel.setAdList(fetchedAds?.toMutableList() ?: mutableListOf())
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch ads: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false // Stop the spinner
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}