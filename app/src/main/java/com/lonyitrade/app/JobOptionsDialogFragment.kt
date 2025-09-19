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
                    "Apply for a Job" -> {
                        Toast.makeText(requireContext(), "Opening 'Apply for Job' form...", Toast.LENGTH_SHORT).show()
                        val applyForJobIntent = Intent(requireContext(), ApplyForJobActivity::class.java)
                        startActivity(applyForJobIntent)
                        dismiss()
                    }
                    "Hire an Employee" -> {
                        Toast.makeText(requireContext(), "Opening 'Hire an Employee' listings...", Toast.LENGTH_SHORT).show()
                        val hireEmployeeIntent = Intent(requireContext(), HireEmployeeActivity::class.java)
                        startActivity(hireEmployeeIntent)
                        dismiss()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please select an option", Toast.LENGTH_SHORT).show()
            }
        }
    }
}