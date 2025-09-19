package com.lonyitrade.app

import android.app.Activity
import android.content.Intent
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
import com.lonyitrade.app.utils.NetworkUtils
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyRentalsFragment : Fragment(R.layout.fragment_my_rentals) {

    private lateinit var myRentalsRecyclerView: RecyclerView
    private lateinit var noRentalsTextView: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MyRentalsAdapter
    private var rentalList: MutableList<Rental> = mutableListOf()
    private lateinit var networkErrorLayout: LinearLayout // NEW: Reference for the network error layout

    private val editRentalLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchMyRentals() // Refresh the rentals list
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        myRentalsRecyclerView = view.findViewById(R.id.myRentalsRecyclerView)
        noRentalsTextView = view.findViewById(R.id.noRentalsTextView)
        networkErrorLayout = view.findViewById(R.id.networkErrorLayout) // NEW: Initialize the network error layout
        myRentalsRecyclerView.layoutManager = LinearLayoutManager(context)

        setupAdapter()
        fetchMyRentals()
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
        myRentalsRecyclerView.adapter = adapter
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
                val response = ApiClient.apiService.getMyRentals("Bearer $token")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val rentals = response.body() ?: emptyList()
                        rentalList.clear()
                        rentalList.addAll(rentals)
                        adapter.notifyDataSetChanged()
                        updateEmptyView(rentals.isEmpty())
                    } else {
                        Toast.makeText(context, "Failed to load rentals", Toast.LENGTH_SHORT).show()
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
                val response = ApiClient.apiService.deleteRental("Bearer $token", rentalId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Rental deleted", Toast.LENGTH_SHORT).show()
                        val position = rentalList.indexOf(rental)
                        if (position != -1) {
                            rentalList.removeAt(position)
                            adapter.notifyItemRemoved(position)
                            updateEmptyView(rentalList.isEmpty())
                        }
                    } else {
                        Toast.makeText(context, "Failed to delete rental", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            myRentalsRecyclerView.visibility = View.GONE
            noRentalsTextView.visibility = View.VISIBLE
        } else {
            myRentalsRecyclerView.visibility = View.VISIBLE
            noRentalsTextView.visibility = View.GONE
        }
    }

    private fun showNetworkError() {
        myRentalsRecyclerView.visibility = View.GONE
        noRentalsTextView.visibility = View.GONE
        networkErrorLayout.visibility = View.VISIBLE
    }

    private fun hideNetworkError() {
        networkErrorLayout.visibility = View.GONE
    }
}