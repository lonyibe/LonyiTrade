package com.lonyitrade.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.R
import com.lonyitrade.app.data.models.Review

class ReviewAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

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
        private val reviewerNameTextView: TextView = itemView.findViewById(R.id.reviewerNameTextView)
        private val reviewRatingBar: RatingBar = itemView.findViewById(R.id.reviewRatingBar)
        private val reviewTextView: TextView = itemView.findViewById(R.id.reviewTextView)

        fun bind(review: Review) {
            reviewerNameTextView.text = review.reviewer_name ?: "Anonymous"
            reviewRatingBar.rating = review.rating.toFloat()
            reviewTextView.text = review.review_text
        }
    }
}