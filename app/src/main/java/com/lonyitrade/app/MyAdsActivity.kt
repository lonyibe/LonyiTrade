package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.MyAdsAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyAdsActivity : AppCompatActivity() {

    private lateinit var myAdsRecyclerView: RecyclerView
    private lateinit var noAdsTextView: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MyAdsAdapter
    private var adList: MutableList<Ad> = mutableListOf()

    // This launcher will refresh the list when you return from the EditAdActivity
    private val editAdLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchMyAds() // Refresh the ads list
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ads)

        sessionManager = SessionManager(this)
        myAdsRecyclerView = findViewById(R.id.myAdsRecyclerView)
        noAdsTextView = findViewById(R.id.noAdsTextView)
        myAdsRecyclerView.layoutManager = LinearLayoutManager(this)

        setupAdapter()
        fetchMyAds()
    }

    private fun setupAdapter() {
        adapter = MyAdsAdapter(
            adList,
            onEditClick = { ad ->
                val intent = Intent(this, EditAdActivity::class.java)
                intent.putExtra("AD_EXTRA", ad)
                editAdLauncher.launch(intent)
            },
            onDeleteClick = { ad, position ->
                showDeleteConfirmationDialog(ad, position)
            }
        )
        myAdsRecyclerView.adapter = adapter
    }

    private fun fetchMyAds() {
        val token = sessionManager.fetchAuthToken()
        if (token != null) {
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
                            Toast.makeText(this@MyAdsActivity, "Failed to load ads", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(ad: Ad, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Ad")
            .setMessage("Are you sure you want to delete this ad? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAd(ad, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAd(ad: Ad, position: Int) {
        val token = sessionManager.fetchAuthToken()
        if (token != null && ad.id != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.apiService.deleteAdvert("Bearer $token", ad.id)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MyAdsActivity, "Ad deleted successfully", Toast.LENGTH_SHORT).show()
                            adapter.removeItem(position)
                            updateEmptyView()
                        } else {
                            Toast.makeText(this@MyAdsActivity, "Failed to delete ad", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
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
}