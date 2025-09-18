package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.RentalAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyRentalsFragment : Fragment(R.layout.fragment_my_rentals) {

    private lateinit var myRentalsRecyclerView: RecyclerView
    private lateinit var noRentalsTextView: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: RentalAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        myRentalsRecyclerView = view.findViewById(R.id.myRentalsRecyclerView)
        noRentalsTextView = view.findViewById(R.id.noRentalsTextView)
        myRentalsRecyclerView.layoutManager = LinearLayoutManager(context)

        fetchMyRentals()
    }

    private fun fetchMyRentals() {
        val token = sessionManager.fetchAuthToken() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getMyRentals("Bearer $token")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val rentals = response.body() ?: emptyList()
                        adapter = RentalAdapter(rentals)
                        myRentalsRecyclerView.adapter = adapter
                        updateEmptyView(rentals.isEmpty())
                    } else {
                        Toast.makeText(context, "Failed to load rentals", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Handle error
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
}