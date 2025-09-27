package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.lonyitrade.app.adapters.JobAdAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.JobAd
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// FIX: New Activity for Job Seekers to view and search job listings
class JobListingActivity : AppCompatActivity() {

    // --- UI Elements ---
    private lateinit var jobSearchOptionsCard: MaterialCardView
    private lateinit var searchTitle: TextView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var noResultsTextView: TextView
    private lateinit var sessionManager: SessionManager

    private val apiService by lazy { ApiClient().getApiService(this) }

    // UI elements for animation
    private lateinit var searchFormLayout: LinearLayout
    private lateinit var collapsedSearchIcon: ImageView
    private lateinit var searchButton: Button
    private lateinit var backButton: ImageView

    // Input Fields
    private lateinit var searchQueryEditText: EditText
    private lateinit var searchLocationEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_listing) // FIX: Use new layout

        sessionManager = SessionManager(this)

        initializeViews()
        setupRecyclerView()

        // Automatically load all jobs on start
        performSearch(isInitialLoad = true)

        searchButton.setOnClickListener {
            performSearch()
            collapseSearchForm()
        }

        collapsedSearchIcon.setOnClickListener {
            expandSearchForm()
        }

        backButton.setOnClickListener {
            if (searchFormLayout.visibility == View.GONE) {
                expandSearchForm()
            } else {
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searchFormLayout.visibility == View.GONE) {
                    expandSearchForm()
                } else {
                    finish()
                }
            }
        })
    }

    private fun initializeViews() {
        jobSearchOptionsCard = findViewById(R.id.jobSearchOptionsCard)
        searchTitle = findViewById(R.id.search_title)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        noResultsTextView = findViewById(R.id.noResultsTextView)

        searchFormLayout = findViewById(R.id.searchFormLayout)
        collapsedSearchIcon = findViewById(R.id.collapsedSearchIcon)
        searchButton = findViewById(R.id.search_button)
        backButton = findViewById(R.id.backButton)

        // Initialize input fields
        searchQueryEditText = findViewById(R.id.search_query)
        searchLocationEditText = findViewById(R.id.search_location)
    }

    private fun setupRecyclerView() {
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun performSearch(isInitialLoad: Boolean = false) {
        val query = searchQueryEditText.text.toString().trim()
        val location = searchLocationEditText.text.toString().trim()

        if (!isInitialLoad) {
            Toast.makeText(this, "Searching jobs...", Toast.LENGTH_SHORT).show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // FIX: Call the new API endpoint for job listings
                val response = apiService.getJobListings(query, location)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val jobAds = response.body()
                        if (jobAds.isNullOrEmpty()) {
                            showNoResults()
                        } else {
                            showJobAdResults(jobAds)
                        }
                    } else {
                        showNoResults()
                        Toast.makeText(this@JobListingActivity, "Search failed: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showNoResults()
                    Toast.makeText(this@JobListingActivity, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun collapseSearchForm() {
        searchFormLayout.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                searchFormLayout.visibility = View.GONE
                searchButton.visibility = View.GONE
                collapsedSearchIcon.visibility = View.VISIBLE
                backButton.visibility = View.VISIBLE
            }
            .start()
    }

    private fun expandSearchForm() {
        searchResultsRecyclerView.visibility = View.GONE
        noResultsTextView.visibility = View.GONE
        searchResultsRecyclerView.adapter = null

        collapsedSearchIcon.visibility = View.GONE
        backButton.visibility = View.GONE
        searchFormLayout.visibility = View.VISIBLE
        searchButton.visibility = View.VISIBLE
        searchFormLayout.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun showJobAdResults(jobAds: List<JobAd>) {
        searchResultsRecyclerView.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE

        // FIX: Use the new JobAdAdapter
        val jobAdAdapter = JobAdAdapter(jobAds) { jobAd ->
            // FIX: Handle click to open a job detail activity (JobDetailActivity is assumed for consistency)
            val intent = Intent(this, JobDetailActivity::class.java).apply {
                putExtra("JOB_AD_EXTRA", jobAd)
            }
            startActivity(intent)
        }
        searchResultsRecyclerView.adapter = jobAdAdapter
    }

    private fun showNoResults() {
        searchResultsRecyclerView.visibility = View.GONE
        noResultsTextView.visibility = View.VISIBLE
    }
}