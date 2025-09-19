package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.AdAdapter
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.SessionManager
import java.util.UUID

class HireEmployeeActivity : AppCompatActivity() {

    private lateinit var searchJobTitle: EditText
    private lateinit var searchEducation: EditText
    private lateinit var searchEmployeesButton: Button
    private lateinit var applicantsRecyclerView: RecyclerView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hire_employee)

        sessionManager = SessionManager(this)
        initializeViews()
        setupListeners()
        setupRecyclerView()
        // Load all applicants initially
        loadApplicants()
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

    private fun loadApplicants() {
        // This is a placeholder for fetching job applications from an API.
        // For now, we'll use dummy data.
        val dummyApplicants = listOf(
            Ad(
                id = UUID.randomUUID().toString(),
                userId = "applicant1",
                type = "for_hire",
                title = "Graphic Designer",
                description = "Experienced graphic designer with a portfolio of over 50 projects. Proficient in Adobe Creative Suite.",
                category = "Services",
                price = "400000",
                priceType = "Fixed",
                district = "Kampala",
                condition = "N/A",
                sellerPhoneNumber = "077-123-4567",
                createdAt = "2024-10-27T10:00:00Z",
                photos = emptyList()
            ),
            Ad(
                id = UUID.randomUUID().toString(),
                userId = "applicant2",
                type = "for_hire",
                title = "Software Developer",
                description = "Recent graduate with a B.Sc. in Computer Science. Skilled in Kotlin and Java. Looking for an entry-level position.",
                category = "Services",
                price = "300000",
                priceType = "Negotiable",
                district = "Mukono",
                condition = "N/A",
                sellerPhoneNumber = "078-765-4321",
                createdAt = "2024-10-26T14:30:00Z",
                photos = emptyList()
            )
        )
        displayApplicants(dummyApplicants)
    }

    private fun performSearch() {
        val jobTitle = searchJobTitle.text.toString().trim()
        val education = searchEducation.text.toString().trim()

        Toast.makeText(this, "Searching for applicants...", Toast.LENGTH_SHORT).show()

        // TODO: Implement actual search logic with API call based on jobTitle and education
        // For now, we'll just reload the dummy data
        loadApplicants()
    }

    private fun displayApplicants(applicants: List<Ad>) {
        val currentUserId = sessionManager.fetchAuthToken()
        val adapter = AdAdapter(applicants, currentUserId) { ad ->
            Toast.makeText(this, "Messaging applicant: ${ad.title}", Toast.LENGTH_SHORT).show()
            // TODO: Implement chat functionality for hiring
        }
        applicantsRecyclerView.adapter = adapter
    }
}