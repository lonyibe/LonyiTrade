package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.RentalAdapter
import com.lonyitrade.app.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RentalsFragment : Fragment() {

    private lateinit var rentalsRecyclerView: RecyclerView
    private lateinit var rentalAdapter: RentalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rentals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rentalsRecyclerView = view.findViewById(R.id.rentalsRecyclerView)
        rentalsRecyclerView.layoutManager = LinearLayoutManager(context)

        fetchRentals()
    }

    private fun fetchRentals() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getRentals()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val rentals = response.body() ?: emptyList()
                        rentalAdapter = RentalAdapter(rentals)
                        rentalsRecyclerView.adapter = rentalAdapter
                    } else {
                        Toast.makeText(requireContext(), "Failed to load rentals", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}