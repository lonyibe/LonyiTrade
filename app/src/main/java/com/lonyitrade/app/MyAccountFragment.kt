package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyAccountFragment : Fragment(R.layout.fragment_my_account) {

    private lateinit var sessionManager: SessionManager
    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)
        val fullNameTextView = view.findViewById<TextView>(R.id.fullNameTextView)
        val phoneNumberTextView = view.findViewById<TextView>(R.id.phoneNumberTextView)
        val districtTextView = view.findViewById<TextView>(R.id.districtTextView)
        val myAdsButton = view.findViewById<Button>(R.id.myAdsButton)
        val myJobApplicationsButton = view.findViewById<Button>(R.id.myJobApplicationsButton)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        fetchUserProfile(profileImageView, fullNameTextView, phoneNumberTextView, districtTextView)

        myAdsButton.setOnClickListener {
            startActivity(Intent(requireContext(), MyAdsActivity::class.java))
        }

        myJobApplicationsButton.setOnClickListener {
            startActivity(Intent(requireContext(), MyJobApplicationsActivity::class.java))
        }

        logoutButton.setOnClickListener {
            sessionManager.logoutUser()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            // Clear all previous activities
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun fetchUserProfile(
        profileImageView: ImageView,
        fullNameTextView: TextView,
        phoneNumberTextView: TextView,
        districtTextView: TextView
    ) {
        val token = sessionManager.fetchAuthToken()
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Correctly use the apiService instance
                    val response = apiService.getUserProfile("Bearer $token")
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val userProfile = response.body()
                            fullNameTextView.text = userProfile?.fullName ?: "N/A"
                            phoneNumberTextView.text = userProfile?.phoneNumber ?: "N/A"
                            districtTextView.text = userProfile?.district ?: "N/A"

                            // Correctly reference the public BASE_URL
                            userProfile?.profilePictureUrl?.let {
                                Glide.with(requireContext())
                                    .load(ApiClient.BASE_URL.trimEnd('/') + it)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .into(profileImageView)
                            }
                        } else {
                            // Handle cases like an expired token
                            Toast.makeText(requireContext(), "Could not load profile. Please log in again.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main){
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}