package com.lonyitrade.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lonyitrade.app.R
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.utils.BlurTransformation // 1. New Import

class AdPhotoAdapter(private val photos: List<String>) : RecyclerView.Adapter<AdPhotoAdapter.PhotoViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foregroundImageView: ImageView = itemView.findViewById(R.id.adImageView)
        val backgroundImageView: ImageView = itemView.findViewById(R.id.backgroundImageView) // 2. Find new ImageView

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ad_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + photos[position].trimStart('/')

        // 3a. Load Foreground Image (Sharp, fitCenter to show the whole image)
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_add_photo)
            .error(R.drawable.ic_add_photo)
            .into(holder.foregroundImageView)

        // 3b. Load Background Image (Blurred, centerCrop to fill the background)
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_add_photo)
            .error(R.drawable.ic_add_photo)
            // Apply the custom blur transformation here
            .transform(BlurTransformation(holder.itemView.context, 15f)) // Set radius (e.g., 15)
            .into(holder.backgroundImageView)
    }

    override fun getItemCount(): Int = photos.size
}