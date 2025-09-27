package com.lonyitrade.app.adapters

import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.lonyitrade.app.R
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.AdDetailActivity
import com.lonyitrade.app.ChatActivity

class AdAdapter(private val adList: List<Ad>, private val currentUserId: String?, private val onMessageSellerClick: (Ad) -> Unit) : RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageContainer: FrameLayout = view.findViewById(R.id.adImageContainer)
        val photoImageView: ImageView = view.findViewById(R.id.adPhotoImageView)
        val progressBar: ProgressBar = view.findViewById(R.id.imageProgressBar)
        val titleTextView: TextView = view.findViewById(R.id.adTitleTextView)
        val categoryTextView: TextView = view.findViewById(R.id.adCategoryTextView)
        val adTypeTextView: TextView = view.findViewById(R.id.adTypeTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.adDescriptionTextView)
        val priceTextView: TextView = view.findViewById(R.id.adPriceTextView)
        val priceTypeTextView: TextView = view.findViewById(R.id.adPriceTypeTextView)
        val conditionTextView: TextView = view.findViewById(R.id.adConditionTextView)
        val locationTextView: TextView = view.findViewById(R.id.adLocationTextView)
        val phoneNumberTextView: TextView = view.findViewById(R.id.adPhoneNumberTextView)
        val messageSellerButton: Button = view.findViewById(R.id.messageSellerButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ad_item_card, parent, false)
        return AdViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        val ad = adList[position]

        // Handle item click to open details page
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, AdDetailActivity::class.java).apply {
                putExtra("AD_EXTRA", ad)
            }
            holder.itemView.context.startActivity(intent)
        }

        // Hide message button if the ad belongs to the current user
        if (ad.userId == currentUserId) {
            holder.messageSellerButton.visibility = View.GONE
        } else {
            holder.messageSellerButton.visibility = View.VISIBLE
            holder.messageSellerButton.setOnClickListener {
                if (ad.userId != null) {
                    onMessageSellerClick(ad)
                } else {
                    Toast.makeText(holder.itemView.context, "Seller ID is missing. Cannot start chat.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle Ad Type and Image Visibility
        if (ad.type == "wanted") {
            holder.imageContainer.visibility = View.GONE // Hide image section for "wanted" ads
        } else {
            holder.imageContainer.visibility = View.VISIBLE
            holder.progressBar.visibility = View.VISIBLE // Show spinner

            if (!ad.photos.isNullOrEmpty() && ad.photos.first() != null) {
                val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + ad.photos.first()!!.trimStart('/')
                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            holder.progressBar.visibility = View.GONE
                            return false // Important to return false so the error placeholder can be displayed
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            holder.progressBar.visibility = View.GONE
                            return false // Important to return false so the image can be displayed
                        }
                    })
                    .error(R.drawable.ic_add_photo)
                    .into(holder.photoImageView)
            } else {
                holder.progressBar.visibility = View.GONE
                holder.photoImageView.setImageResource(R.drawable.ic_add_photo)
            }
        }

        // Set Ad Details
        holder.titleTextView.text = ad.title
        holder.categoryTextView.text = "Category: ${ad.category}"
        holder.descriptionTextView.text = ad.description
        holder.priceTextView.text = "UGX ${ad.price ?: "0"}"
        holder.priceTypeTextView.text = ad.priceType ?: ""
        holder.conditionTextView.text = "Condition: ${ad.condition ?: "N/A"}"
        holder.locationTextView.text = ad.district ?: "N/A"
        holder.phoneNumberTextView.text = ad.sellerPhoneNumber ?: "N/A"

        // Set Ad Type Badge
        when (ad.type) {
            "for_sale" -> {
                holder.adTypeTextView.text = "For Sale"
                holder.adTypeTextView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.status_success_green))
            }
            "wanted" -> {
                holder.adTypeTextView.text = "Wanted"
                holder.adTypeTextView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.error_red)) // Corrected
                holder.priceTextView.text = "Budget: UGX ${ad.price ?: "0"}"
            }
            else -> {
                holder.adTypeTextView.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = adList.size
}