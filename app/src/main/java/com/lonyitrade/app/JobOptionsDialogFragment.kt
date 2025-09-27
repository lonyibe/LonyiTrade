package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class JobOptionsDialogFragment : DialogFragment() {

    // FIX: Add constants for the new activity names that will be created
    companion object {
        const val OPTION_APPLY_FOR_JOB = "Apply for a Job"
        const val OPTION_HIRE_EMPLOYEE = "Hire an Employee (View Applicants)"
        const val OPTION_FIND_JOB_LISTING = "Find Job" // FIX: New option for job seekers to find listings
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // NOTE: The corresponding layout file R.layout.dialog_job_options must be updated
        // to include the new radio button for "Find Job".
        return inflater.inflate(R.layout.dialog_job_options, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.jobOptionRadioGroup)
        val continueButton = view.findViewById<Button>(R.id.continueButton)

        continueButton.setOnClickListener {
            val selectedOptionId = radioGroup.checkedRadioButtonId
            if (selectedOptionId != -1) {
                val selectedRadioButton = view.findViewById<RadioButton>(selectedOptionId)
                val selectedText = selectedRadioButton.text.toString()

                when (selectedText) {
                    OPTION_APPLY_FOR_JOB -> {
                        Toast.makeText(requireContext(), "Opening 'Apply for Job' form...", Toast.LENGTH_SHORT).show()
                        val applyForJobIntent = Intent(requireContext(), ApplyForJobActivity::class.java)
                        startActivity(applyForJobIntent)
                        dismiss()
                    }
                    OPTION_HIRE_EMPLOYEE -> {
                        // This option now explicitly states it's for viewing applicants
                        Toast.makeText(requireContext(), "Opening 'Hire an Employee' listings...", Toast.LENGTH_SHORT).show()
                        val hireEmployeeIntent = Intent(requireContext(), HireEmployeeActivity::class.java)
                        startActivity(hireEmployeeIntent)
                        dismiss()
                    }
                    OPTION_FIND_JOB_LISTING -> {
                        // FIX: New functionality for job seekers to view posted job listings
                        Toast.makeText(requireContext(), "Opening 'Job Listings' search...", Toast.LENGTH_SHORT).show()
                        // FIX: Intent will open a new activity called JobListingActivity (to be created)
                        val findJobIntent = Intent(requireContext(), JobListingActivity::class.java)
                        startActivity(findJobIntent)
                        dismiss()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please select an option", Toast.LENGTH_SHORT).show()
            }
        }
    }
}