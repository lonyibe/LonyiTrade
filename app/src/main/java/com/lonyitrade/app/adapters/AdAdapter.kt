package com.lonyitrade.app.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.R
import com.lonyitrade.app.data.models.Ad

class AdAdapter(private val adList: List<Ad>) : RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImageView: ImageView = view.findViewById(R.id.adPhotoImageView)
        val titleTextView: TextView = view.findViewById(R.id.adTitleTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.adDescriptionTextView)
        val priceTextView: TextView = view.findViewById(R.id.adPriceTextView)
        val conditionTextView: TextView = view.findViewById(R.id.adConditionTextView)
        val locationTextView: TextView = view.findViewById(R.id.adLocationTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ad_item_card, parent, false)
        return AdViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        val ad = adList[position]

        // Display the first image if available, otherwise show a placeholder
        if (ad.photos.isNotEmpty()) {
            holder.photoImageView.setImageURI(Uri.parse(ad.photos.first()))
        } else {
            holder.photoImageView.setImageResource(R.drawable.ic_add_photo) // Use a placeholder drawable
        }

        holder.titleTextView.text = ad.title
        holder.descriptionTextView.text = ad.description

        if (ad.type == "sell") {
            holder.priceTextView.text = "UGX ${ad.price}"
        } else {
            holder.priceTextView.text = "Budget: UGX ${ad.budget}"
        }

        holder.conditionTextView.text = "Condition: ${ad.condition}"
        holder.locationTextView.text = ad.district
    }

    override fun getItemCount() = adList.size
}