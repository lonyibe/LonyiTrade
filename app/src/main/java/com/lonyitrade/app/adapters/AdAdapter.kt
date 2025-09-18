package com.lonyitrade.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lonyitrade.app.R
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.api.ApiClient

class AdAdapter(private val adList: List<Ad>) : RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImageView: ImageView = view.findViewById(R.id.adPhotoImageView)
        val titleTextView: TextView = view.findViewById(R.id.adTitleTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.adDescriptionTextView)
        val priceTextView: TextView = view.findViewById(R.id.adPriceTextView)
        val priceTypeTextView: TextView = view.findViewById(R.id.adPriceTypeTextView)
        val conditionTextView: TextView = view.findViewById(R.id.adConditionTextView)
        val locationTextView: TextView = view.findViewById(R.id.adLocationTextView)
        val phoneNumberTextView: TextView = view.findViewById(R.id.adPhoneNumberTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ad_item_card, parent, false)
        return AdViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        val ad = adList[position]

        if (!ad.photos.isNullOrEmpty() && ad.photos.first() != null) {
            val imagePath = ad.photos.first()
            val imageUrl = if (imagePath.startsWith("http")) {
                imagePath
            } else {
                ApiClient.BASE_URL.trimEnd('/') + "/" + imagePath.trimStart('/')
            }
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_add_photo)
                .error(R.drawable.ic_add_photo)
                .into(holder.photoImageView)
        } else {
            holder.photoImageView.setImageResource(R.drawable.ic_add_photo)
        }

        holder.titleTextView.text = ad.title
        holder.descriptionTextView.text = ad.description
        holder.priceTextView.text = "UGX ${ad.price}"
        holder.priceTypeTextView.text = ad.priceType ?: ""
        holder.conditionTextView.text = "Condition: ${ad.condition ?: "N/A"}"
        holder.locationTextView.text = ad.district
        holder.phoneNumberTextView.text = ad.sellerPhoneNumber ?: "N/A"
    }

    override fun getItemCount() = adList.size
}