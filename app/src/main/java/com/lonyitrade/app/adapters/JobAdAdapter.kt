package com.lonyitrade.app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.R
import com.lonyitrade.app.data.models.JobAd
import com.lonyitrade.app.JobDetailActivity // Assuming a new detail activity for jobs

// FIX: Adapted from AdAdapter.kt to handle JobAd objects.
class JobAdAdapter(private val jobAdList: List<JobAd>, private val onJobAdClick: (JobAd) -> Unit) : RecyclerView.Adapter<JobAdAdapter.JobAdViewHolder>() {

    class JobAdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // FIX: Simplified UI elements for a job listing (must match job_ad_item_card.xml)
        val titleTextView: TextView = view.findViewById(R.id.jobTitleTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.jobDescriptionTextView)
        val locationTextView: TextView = view.findViewById(R.id.jobLocationTextView)
        val applyButton: Button = view.findViewById(R.id.applyForJobButton) // Assuming this button exists
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobAdViewHolder {
        // FIX: Uses a new layout specifically for job ads
        val view = LayoutInflater.from(parent.context).inflate(R.layout.job_ad_item_card, parent, false)
        return JobAdViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobAdViewHolder, position: Int) {
        val jobAd = jobAdList[position]

        // Handle item click to open details page
        holder.itemView.setOnClickListener {
            onJobAdClick(jobAd)
        }

        // Handle Apply Button click (can be changed to launch ApplyForJobActivity later)
        holder.applyButton.setOnClickListener {
            onJobAdClick(jobAd)
        }

        // Set Job Ad Details
        holder.titleTextView.text = jobAd.title
        holder.descriptionTextView.text = jobAd.description
        holder.locationTextView.text = "Location: ${jobAd.location}"
    }

    override fun getItemCount() = jobAdList.size
}