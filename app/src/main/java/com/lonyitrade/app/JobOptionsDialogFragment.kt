// File: lonyibe/lonyitrade/LonyiTrade-972927e6330d9227af4b84243941aa2c481cafba/app/src/main/java/com/lonyitrade/app/JobOptionsDialogFragment.kt

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

    companion object {
        const val OPTION_APPLY_FOR_JOB = "Apply for a Job"
        // FIX: Changed constant to match the actual text in dialog_job_options.xml ("Hire an Employee")
        const val OPTION_HIRE_EMPLOYEE = "Hire an Employee"
        const val OPTION_FIND_JOB_LISTING = "Find Job"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
                    // FIX APPLIED HERE: The logic block is now reached correctly
                    OPTION_HIRE_EMPLOYEE -> {
                        Toast.makeText(requireContext(), "Opening 'Hire an Employee' listings...", Toast.LENGTH_SHORT).show()
                        val hireEmployeeIntent = Intent(requireContext(), HireEmployeeActivity::class.java)
                        startActivity(hireEmployeeIntent)
                        dismiss()
                    }
                    OPTION_FIND_JOB_LISTING -> {
                        Toast.makeText(requireContext(), "Opening 'Job Listings' search...", Toast.LENGTH_SHORT).show()
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