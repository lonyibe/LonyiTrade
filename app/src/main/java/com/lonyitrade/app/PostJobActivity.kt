package com.lonyitrade.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.JobListingRequest
import com.lonyitrade.app.utils.LoadingDialogFragment
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostJobActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val apiService by lazy { ApiClient().getApiService(this) }
    private val loadingDialog by lazy { LoadingDialogFragment() }

    // UI elements
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var postJobButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_job) // FIX: Changed layout reference

        sessionManager = SessionManager(this)
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        titleEditText = findViewById(R.id.titleEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        locationEditText = findViewById(R.id.locationEditText) // FIX: New field
        postJobButton = findViewById(R.id.postJobButton) // FIX: Changed ID

        // FIX: Removed references to price, priceType, contact info, condition, and photos.
    }

    private fun setupListeners() {
        postJobButton.setOnClickListener {
            attemptPostJob()
        }
    }

    private fun attemptPostJob() {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            Toast.makeText(this, "Authentication required to post a job.", Toast.LENGTH_LONG).show()
            return
        }

        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()

        if (title.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val jobListingRequest = JobListingRequest(title, description, location)

        loadingDialog.show(supportFragmentManager, "loading")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.postJobListing("Bearer $token", jobListingRequest)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful) {
                        Toast.makeText(this@PostJobActivity, "Job Listing Posted Successfully!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(this@PostJobActivity, "Failed to post job: $errorBody", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@PostJobActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}