package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.JobApplicationRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApplyForJobActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var jobInterestedEditText: EditText
    private lateinit var educationEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var applyButton: Button
    private lateinit var applyProgressBar: ProgressBar
    private lateinit var sessionManager: SessionManager
    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apply_for_job)

        sessionManager = SessionManager(this)
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        nameEditText = findViewById(R.id.nameEditText)
        jobInterestedEditText = findViewById(R.id.jobInterestedEditText)
        educationEditText = findViewById(R.id.educationEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        applyButton = findViewById(R.id.applyButton)
        applyProgressBar = findViewById(R.id.applyProgressBar)
    }

    private fun setupListeners() {
        applyButton.setOnClickListener {
            handleJobApplication()
        }
    }

    private fun handleJobApplication() {
        val name = nameEditText.text.toString().trim()
        val job = jobInterestedEditText.text.toString().trim()
        val education = educationEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        if (name.isEmpty() || job.isEmpty() || education.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            Toast.makeText(this, "You must be logged in to apply", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        val jobApplicationRequest = JobApplicationRequest(
            fullName = name,
            phoneNumber = phone,
            jobInterestedIn = job,
            educationLevel = education
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use the correctly initialized apiService
                val response = apiService.applyForJob("Bearer $token", jobApplicationRequest)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@ApplyForJobActivity, "Application submitted successfully!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@ApplyForJobActivity, "Failed to submit application: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    // Corrected the label here
                    Toast.makeText(this@ApplyForJobActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            applyButton.text = ""
            applyProgressBar.visibility = View.VISIBLE
            applyButton.isEnabled = false
        } else {
            applyButton.text = "Apply"
            applyProgressBar.visibility = View.GONE
            applyButton.isEnabled = true
        }
    }
}