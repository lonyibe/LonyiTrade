package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.JobApplicationAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.launch

class MyJobApplicationsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var jobApplicationAdapter: JobApplicationAdapter
    private lateinit var jobApplicationsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noApplicationsTextView: TextView

    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_job_applications)

        sessionManager = SessionManager(this)
        jobApplicationsRecyclerView = findViewById(R.id.jobApplicationsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        noApplicationsTextView = findViewById(R.id.noApplicationsTextView)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        setupRecyclerView()
        fetchJobApplications()
    }

    private fun setupRecyclerView() {
        jobApplicationAdapter = JobApplicationAdapter(emptyList())
        jobApplicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        jobApplicationsRecyclerView.adapter = jobApplicationAdapter
    }

    private fun fetchJobApplications() {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Correctly use the apiService instance
                val response = apiService.getMyJobApplications("Bearer $token")
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val applications = response.body() ?: emptyList()
                    if (applications.isEmpty()) {
                        noApplicationsTextView.visibility = View.VISIBLE
                        jobApplicationsRecyclerView.visibility = View.GONE
                    } else {
                        noApplicationsTextView.visibility = View.GONE
                        jobApplicationsRecyclerView.visibility = View.VISIBLE
                        jobApplicationAdapter.updateApplications(applications)
                    }
                } else {
                    Toast.makeText(this@MyJobApplicationsActivity, "Failed to load applications: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@MyJobApplicationsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}