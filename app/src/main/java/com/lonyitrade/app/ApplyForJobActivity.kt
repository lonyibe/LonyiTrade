package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ApplyForJobActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var jobInterestedEditText: EditText
    private lateinit var educationEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var applyButton: Button
    private lateinit var applyProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apply_for_job)

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

        showLoading(true)
        // TODO: Implement API call to submit job application
        Toast.makeText(this, "Application submitted successfully! (Logic to be implemented)", Toast.LENGTH_LONG).show()
        showLoading(false)
        finish()
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