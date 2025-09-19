package com.lonyitrade.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.MyAdsAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.NetworkChangeReceiver
import com.lonyitrade.app.utils.NetworkUtils
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyAdsFragment : Fragment(R.layout.fragment_my_ads) {

    private lateinit var myAdsRecyclerView: RecyclerView
    private lateinit var noAdsTextView: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MyAdsAdapter
    private var adList: MutableList<Ad> = mutableListOf()
    private lateinit var networkErrorLayout: LinearLayout

    // NEW: NetworkChangeReceiver instance
    private lateinit var networkChangeReceiver: NetworkChangeReceiver

    private val editAdLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchMyAds() // Refresh the ads list
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        myAdsRecyclerView = view.findViewById(R.id.myAdsRecyclerView)
        noAdsTextView = view.findViewById(R.id.noAdsTextView)
        networkErrorLayout = view.findViewById(R.id.networkErrorLayout)
        myAdsRecyclerView.layoutManager = LinearLayoutManager(context)

        // NEW: Initialize the receiver with both onNetworkAvailable and onNetworkLost callbacks
        networkChangeReceiver = NetworkChangeReceiver(
            onNetworkAvailable = {
                fetchMyAds()
            },
            onNetworkLost = {
                showNetworkError()
            }
        )

        setupAdapter()
        fetchMyAds()
    }

    // NEW: Register the receiver when the fragment is started
    override fun onStart() {
        super.onStart()
        requireContext().registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    // NEW: Unregister the receiver when the fragment is stopped
    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(networkChangeReceiver)
    }

    private fun setupAdapter() {
        adapter = MyAdsAdapter(
            adList,
            onEditClick = { ad ->
                val intent = Intent(requireContext(), EditAdActivity::class.java).apply {
                    putExtra("AD_EXTRA", ad)
                }
                editAdLauncher.launch(intent)
            },
            onDeleteClick = { ad, _ ->
                showDeleteConfirmationDialog(ad)
            }
        )
        myAdsRecyclerView.adapter = adapter
    }

    private fun fetchMyAds() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            showNetworkError()
            return
        }

        hideNetworkError()

        val token = sessionManager.fetchAuthToken() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getMyAdverts("Bearer $token")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val ads = response.body() ?: emptyList()
                        adList.clear()
                        adList.addAll(ads)
                        adapter.notifyDataSetChanged()
                        updateEmptyView()
                    } else {
                        Toast.makeText(context, "Failed to load ads", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                    showNetworkError()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(ad: Ad) {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(context, "No internet connection. Cannot delete ad.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Ad")
            .setMessage("Are you sure you want to delete this ad?")
            .setPositiveButton("Delete") { _, _ -> deleteAd(ad) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAd(ad: Ad) {
        val token = sessionManager.fetchAuthToken() ?: return
        val adId = ad.id ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.deleteAdvert("Bearer $token", adId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Ad deleted", Toast.LENGTH_SHORT).show()
                        val position = adList.indexOf(ad)
                        if (position != -1) {
                            adList.removeAt(position)
                            adapter.notifyItemRemoved(position)
                            updateEmptyView()
                        }
                    } else {
                        Toast.makeText(context, "Failed to delete ad", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateEmptyView() {
        if (adList.isEmpty()) {
            myAdsRecyclerView.visibility = View.GONE
            noAdsTextView.visibility = View.VISIBLE
        } else {
            myAdsRecyclerView.visibility = View.VISIBLE
            noAdsTextView.visibility = View.GONE
        }
    }

    private fun showNetworkError() {
        myAdsRecyclerView.visibility = View.GONE
        noAdsTextView.visibility = View.GONE
        networkErrorLayout.visibility = View.VISIBLE
    }

    private fun hideNetworkError() {
        networkErrorLayout.visibility = View.GONE
    }
}