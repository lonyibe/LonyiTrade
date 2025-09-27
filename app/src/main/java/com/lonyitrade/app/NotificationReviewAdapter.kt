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
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.ReviewNotification

class NotificationReviewAdapter(
    private var reviews: List<ReviewNotification>,
    private val onItemClick: (ReviewNotification) -> Unit // Click handler for navigation
) : RecyclerView.Adapter<NotificationReviewAdapter.NotificationViewHolder>() {

    fun updateReviews(newReviews: List<ReviewNotification>) {
        this.reviews = newReviews
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        // Reuses the review item layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.review_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val review = reviews[position]
        holder.bind(review, onItemClick)
    }

    override fun getItemCount(): Int = reviews.size

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views present in review_item.xml:
        private val reviewerPhotoImageView: ImageView = itemView.findViewById(R.id.reviewerPhotoImageView)
        private val reviewerNameTextView: TextView = itemView.findViewById(R.id.reviewerNameTextView)
        private val reviewRatingBar: RatingBar = itemView.findViewById(R.id.reviewRatingBar)
        private val reviewTextView: TextView = itemView.findViewById(R.id.reviewTextView)
        private val reviewTimestampTextView: TextView = itemView.findViewById(R.id.reviewTimestampTextView)

        // NOTE: This TextView must be added to your review_item.xml layout file.
        private val adTitleTextView: TextView? = itemView.findViewById(R.id.adTitleTextView)

        fun bind(review: ReviewNotification, onItemClick: (ReviewNotification) -> Unit) {

            // Set the main text fields
            reviewerNameTextView.text = review.reviewer_name ?: "New Review"
            reviewRatingBar.rating = review.rating.toFloat()
            reviewTextView.text = review.review_text

            // Display Ad Title in the notification list item
            adTitleTextView?.text = "Reviewed on: ${review.advert_title ?: "Advert Unavailable"}"

            // Format timestamp
            val date = review.created_at.split("T").firstOrNull() ?: review.created_at
            reviewTimestampTextView.text = "on ${date}"

            // Load profile picture
            val context = itemView.context
            if (!review.reviewer_photo_url.isNullOrEmpty()) {
                val photoUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + review.reviewer_photo_url!!.trimStart('/')
                Glide.with(context)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(reviewerPhotoImageView)
            } else {
                reviewerPhotoImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }

            itemView.setOnClickListener {
                onItemClick(review)
            }
        }
    }
}