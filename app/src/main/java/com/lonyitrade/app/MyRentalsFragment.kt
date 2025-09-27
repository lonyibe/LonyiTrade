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
import com.lonyitrade.app.adapters.MyRentalsAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Rental
import com.lonyitrade.app.utils.NetworkChangeReceiver
import com.lonyitrade.app.utils.NetworkUtils
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyRentalsFragment : Fragment(R.layout.fragment_my_rentals) {

    private var myRentalsRecyclerView: RecyclerView? = null
    private var noRentalsTextView: TextView? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MyRentalsAdapter
    private var rentalList: MutableList<Rental> = mutableListOf()
    private var networkErrorLayout: LinearLayout? = null

    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(requireContext()) }

    private lateinit var networkChangeReceiver: NetworkChangeReceiver

    private val editRentalLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchMyRentals()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        myRentalsRecyclerView = view.findViewById(R.id.myRentalsRecyclerView)
        noRentalsTextView = view.findViewById(R.id.noRentalsTextView)
        networkErrorLayout = view.findViewById(R.id.networkErrorLayout)
        myRentalsRecyclerView?.layoutManager = LinearLayoutManager(context)

        setupAdapter()

        networkChangeReceiver = NetworkChangeReceiver(
            onNetworkAvailable = { fetchMyRentals() },
            onNetworkLost = { showNetworkError() }
        )

        fetchMyRentals()
    }

    override fun onStart() {
        super.onStart()
        requireContext().registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(networkChangeReceiver)
    }

    private fun setupAdapter() {
        adapter = MyRentalsAdapter(
            rentalList,
            onEditClick = { rental ->
                val intent = Intent(requireContext(), EditRentalActivity::class.java).apply {
                    putExtra("RENTAL_EXTRA", rental)
                }
                editRentalLauncher.launch(intent)
            },
            onDeleteClick = { rental, _ ->
                showDeleteConfirmationDialog(rental)
            }
        )
        myRentalsRecyclerView?.adapter = adapter
    }

    private fun fetchMyRentals() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            showNetworkError()
            return
        }

        hideNetworkError()

        val token = sessionManager.fetchAuthToken() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Correctly use the apiService instance
                val response = apiService.getMyRentals("Bearer $token")
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        if (response.isSuccessful) {
                            val rentals = response.body() ?: emptyList()
                            rentalList.clear()
                            rentalList.addAll(rentals)
                            adapter.notifyDataSetChanged()
                            updateEmptyView(rentals.isEmpty())
                        } else {
                            Toast.makeText(context, "Failed to load rentals: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        showNetworkError()
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(rental: Rental) {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(context, "No internet connection. Cannot delete rental.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Rental")
            .setMessage("Are you sure you want to delete this rental listing?")
            .setPositiveButton("Delete") { _, _ -> deleteRental(rental) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRental(rental: Rental) {
        val token = sessionManager.fetchAuthToken() ?: return
        val rentalId = rental.id ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Correctly use the apiService instance
                val response = apiService.deleteRental("Bearer $token", rentalId)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Rental deleted", Toast.LENGTH_SHORT).show()
                            val position = rentalList.indexOf(rental)
                            if (position != -1) {
                                rentalList.removeAt(position)
                                adapter.notifyItemRemoved(position)
                                updateEmptyView(rentalList.isEmpty())
                            }
                        } else {
                            Toast.makeText(context, "Failed to delete rental: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (view == null) return

        if (isEmpty) {
            myRentalsRecyclerView?.visibility = View.GONE
            noRentalsTextView?.visibility = View.VISIBLE
        } else {
            myRentalsRecyclerView?.visibility = View.VISIBLE
            noRentalsTextView?.visibility = View.GONE
        }
    }

    private fun showNetworkError() {
        if (view == null) return
        myRentalsRecyclerView?.visibility = View.GONE
        noRentalsTextView?.visibility = View.GONE
        networkErrorLayout?.visibility = View.VISIBLE
    }

    private fun hideNetworkError() {
        if (view == null) return
        networkErrorLayout?.visibility = View.GONE
    }
}