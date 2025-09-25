package com.lonyitrade.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.JobApplicationAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.JobApplication
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HireEmployeeActivity : AppCompatActivity() {

    private lateinit var searchJobTitle: EditText
    private lateinit var searchEducation: EditText
    private lateinit var searchEmployeesButton: Button
    private lateinit var applicantsRecyclerView: RecyclerView
    private lateinit var sessionManager: SessionManager
    private var allJobApplications: List<JobApplication> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hire_employee)

        sessionManager = SessionManager(this)
        initializeViews()
        setupListeners()
        setupRecyclerView()
        loadJobApplications()
    }

    private fun initializeViews() {
        searchJobTitle = findViewById(R.id.searchJobTitle)
        searchEducation = findViewById(R.id.searchEducation)
        searchEmployeesButton = findViewById(R.id.searchEmployeesButton)
        applicantsRecyclerView = findViewById(R.id.applicantsRecyclerView)
    }

    private fun setupListeners() {
        searchEmployeesButton.setOnClickListener {
            performSearch()
        }
    }

    private fun setupRecyclerView() {
        applicantsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadJobApplications() {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Corrected the method call to getJobApplicants
                val response = ApiClient.apiService.getJobApplicants("Bearer $token", null, null)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val applications = response.body() ?: emptyList()
                        allJobApplications = applications
                        displayJobApplications(applications)
                    } else {
                        Toast.makeText(this@HireEmployeeActivity, "Failed to load applicants", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HireEmployeeActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performSearch() {
        val jobTitle = searchJobTitle.text.toString().trim()
        val education = searchEducation.text.toString().trim()

        val filteredList = allJobApplications.filter {
            (jobTitle.isEmpty() || it.jobInterestedIn.contains(jobTitle, ignoreCase = true)) &&
                    (education.isEmpty() || it.educationLevel.contains(education, ignoreCase = true))
        }
        displayJobApplications(filteredList)
    }

    private fun displayJobApplications(applications: List<JobApplication>) {
        val adapter = JobApplicationAdapter(applications)
        applicantsRecyclerView.adapter = adapter
    }
}