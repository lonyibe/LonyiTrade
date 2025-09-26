package com.lonyitrade.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lonyitrade.app.R
import com.lonyitrade.app.data.models.Review
import com.lonyitrade.app.api.ApiClient // <--- NEW: Import ApiClient

class ReviewAdapter(private var reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    /**
     * Updates the list of reviews and notifies the adapter to refresh the RecyclerView.
     */
    fun updateReviews(newReviews: List<Review>) {
        this.reviews = newReviews
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.review_item, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Assuming review_item.xml has these IDs:
        private val reviewerPhotoImageView: ImageView = itemView.findViewById(R.id.reviewerPhotoImageView)
        private val reviewerNameTextView: TextView = itemView.findViewById(R.id.reviewerNameTextView)
        private val reviewRatingBar: RatingBar = itemView.findViewById(R.id.reviewRatingBar)
        private val reviewTextView: TextView = itemView.findViewById(R.id.reviewTextView)
        private val reviewTimestampTextView: TextView = itemView.findViewById(R.id.reviewTimestampTextView)

        fun bind(review: Review) {
            reviewerNameTextView.text = review.reviewer_name ?: "Anonymous"
            reviewRatingBar.rating = review.rating.toFloat()
            reviewTextView.text = review.review_text

            // Basic date formatting (assumes created_at is an ISO timestamp string)
            val date = review.created_at.split("T").firstOrNull() ?: review.created_at
            reviewTimestampTextView.text = date

            // Load profile picture using Glide
            val context = itemView.context
            if (!review.reviewer_photo_url.isNullOrEmpty()) {
                // CORE FIX START
                val photoUrl = if (review.reviewer_photo_url.startsWith("http", ignoreCase = true)) {
                    // Check if it's already an absolute URL (highly defensive check)
                    review.reviewer_photo_url
                } else {
                    // CONSTRUCT ABSOLUTE URL: Prepend the base URL for asset loading.
                    // The .trimEnd('/') and .trimStart('/') ensure clean concatenation.
                    ApiClient.BASE_URL.trimEnd('/') + "/" + review.reviewer_photo_url.trimStart('/')
                }
                // CORE FIX END

                Glide.with(context)
                    .load(photoUrl) // <--- Use the correctly resolved URL
                    .placeholder(R.drawable.ic_profile_placeholder) // Fallback while loading
                    .error(R.drawable.ic_profile_placeholder) // Fallback on error
                    .circleCrop()
                    .into(reviewerPhotoImageView)
            } else {
                // Set default placeholder if URL is missing
                reviewerPhotoImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }
}