package com.lonyitrade.app.adapters

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
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

class MyAdsAdapter(
    private var adList: MutableList<Ad>,
    private val onEditClick: (Ad) -> Unit,
    private val onDeleteClick: (Ad, Int) -> Unit
) : RecyclerView.Adapter<MyAdsAdapter.AdViewHolder>() {

    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Existing views
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

        // New views for buttons
        val editDeleteLayout: LinearLayout = view.findViewById(R.id.editDeleteLayout)
        val editButton: Button = view.findViewById(R.id.editButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
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

        // --- Bind data ---
        holder.titleTextView.text = ad.title
        holder.categoryTextView.text = "Category: ${ad.category}"
        holder.descriptionTextView.text = ad.description
        holder.priceTextView.text = "UGX ${ad.price ?: "0"}"
        holder.priceTypeTextView.text = ad.priceType ?: ""
        holder.conditionTextView.text = "Condition: ${ad.condition ?: "N/A"}"
        holder.locationTextView.text = ad.district ?: "N/A"
        holder.phoneNumberTextView.text = ad.sellerPhoneNumber ?: "N/A"

        // --- Show Edit/Delete buttons and hide Message button ---
        holder.editDeleteLayout.visibility = View.VISIBLE
        holder.messageSellerButton.visibility = View.GONE

        // --- Set click listeners ---
        holder.editButton.setOnClickListener { onEditClick(ad) }
        holder.deleteButton.setOnClickListener { onDeleteClick(ad, position) }

        // Handle Ad Type and Image Visibility
        if (ad.type == "wanted") {
            holder.imageContainer.visibility = View.GONE
        } else {
            holder.imageContainer.visibility = View.VISIBLE
            holder.progressBar.visibility = View.VISIBLE

            if (!ad.photos.isNullOrEmpty() && ad.photos.first() != null) {
                val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + ad.photos.first()!!.trimStart('/')
                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                            holder.progressBar.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            holder.progressBar.visibility = View.GONE
                            return false
                        }
                    })
                    .error(R.drawable.ic_add_photo)
                    .into(holder.photoImageView)
            } else {
                holder.progressBar.visibility = View.GONE
                holder.photoImageView.setImageResource(R.drawable.ic_add_photo)
            }
        }

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
            else -> holder.adTypeTextView.visibility = View.GONE
        }
    }

    override fun getItemCount() = adList.size

    fun removeItem(position: Int) {
        if (position >= 0 && position < adList.size) {
            adList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}