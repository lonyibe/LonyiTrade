package com.lonyitrade.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReviewActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var ratingBar: RatingBar
    private lateinit var reviewEditText: EditText
    private lateinit var submitReviewButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        backButton = findViewById(R.id.backButton)
        ratingBar = findViewById(R.id.ratingBar)
        reviewEditText = findViewById(R.id.reviewEditText)
        submitReviewButton = findViewById(R.id.submitReviewButton)

        backButton.setOnClickListener {
            finish()
        }

        submitReviewButton.setOnClickListener {
            val rating = ratingBar.rating
            val review = reviewEditText.text.toString()

            if (rating > 0 && review.isNotEmpty()) {
                // Here you would typically send the review to your backend API
                Toast.makeText(this, "Review submitted! Thank you.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please provide a rating and a review.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}