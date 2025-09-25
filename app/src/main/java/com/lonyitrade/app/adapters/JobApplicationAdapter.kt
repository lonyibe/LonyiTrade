package com.lonyitrade.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.R
import com.lonyitrade.app.data.models.JobApplication
import java.text.SimpleDateFormat
import java.util.*

class JobApplicationAdapter(private var applications: List<JobApplication>) :
    RecyclerView.Adapter<JobApplicationAdapter.JobApplicationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job_application, parent, false)
        return JobApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobApplicationViewHolder, position: Int) {
        holder.bind(applications[position])
    }

    override fun getItemCount(): Int = applications.size

    fun updateApplications(newApplications: List<JobApplication>) {
        applications = newApplications
        notifyDataSetChanged()
    }

    class JobApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val jobTitleTextView: TextView = itemView.findViewById(R.id.jobTitleTextView)
        private val applicantNameTextView: TextView = itemView.findViewById(R.id.applicantName)
        private val applicantPhoneNumberTextView: TextView = itemView.findViewById(R.id.applicantPhoneNumber) // New
        private val applicantEducationLevelTextView: TextView = itemView.findViewById(R.id.applicantEducationLevel) // New
        private val applicationDateTextView: TextView = itemView.findViewById(R.id.applicationDate)

        fun bind(application: JobApplication) {
            jobTitleTextView.text = application.jobInterestedIn
            applicantNameTextView.text = application.fullName
            applicantPhoneNumberTextView.text = application.phoneNumber // New
            applicantEducationLevelTextView.text = application.educationLevel // New

            // Format and display the date
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            try {
                val date = inputFormat.parse(application.createdAt)
                applicationDateTextView.text = date?.let { outputFormat.format(it) } ?: "N/A"
            } catch (e: Exception) {
                applicationDateTextView.text = "N/A"
            }
        }
    }
}