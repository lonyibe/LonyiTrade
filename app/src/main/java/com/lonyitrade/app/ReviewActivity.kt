package com.lonyitrade.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.ReviewAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.api.ApiService
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.Review
import com.lonyitrade.app.data.models.ReviewRequest
import com.lonyitrade.app.utils.LoadingDialogFragment
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewActivity : AppCompatActivity() {

    private lateinit var ad: Ad
    private lateinit var backButton: ImageView
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var noReviewsTextView: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var reviewEditText: EditText
    private lateinit var submitReviewButton: Button
    private lateinit var submitReviewContainer: View
    private lateinit var reviewAdapter: ReviewAdapter

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private val loadingDialog = LoadingDialogFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        // Get Ad data passed from AdDetailActivity
        ad = intent.getParcelableExtra("AD_EXTRA") ?: run {
            Toast.makeText(this, "Error loading ad data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        sessionManager = SessionManager(this)
        apiService = ApiClient.getApiService(this)

        initializeViews()
        setupReviewList()
        fetchReviews()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView)
        noReviewsTextView = findViewById(R.id.noReviewsTextView)
        ratingBar = findViewById(R.id.ratingBar)
        reviewEditText = findViewById(R.id.reviewEditText)
        submitReviewButton = findViewById(R.id.submitReviewButton)
        submitReviewContainer = findViewById(R.id.submitReviewContainer)

        findViewById<TextView>(R.id.reviewHeaderTextView).text = "Reviews for ${ad.title}"

        backButton.setOnClickListener {
            finish()
        }

        // Only allow submitting a review if the current user is NOT the seller/user being reviewed
        val currentUserId = sessionManager.getUserId()
        if (ad.userId == currentUserId) {
            submitReviewContainer.visibility = View.GONE
        } else {
            submitReviewContainer.visibility = View.VISIBLE
            submitReviewButton.setOnClickListener {
                postUserReview()
            }
        }
    }

    private fun setupReviewList() {
        reviewAdapter = ReviewAdapter(emptyList())
        reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReviewActivity)
            adapter = reviewAdapter
        }
    }

    private fun fetchReviews() {
        if (!loadingDialog.isAdded) {
            loadingDialog.show(supportFragmentManager, "loading")
        }

        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            loadingDialog.dismiss()
            Toast.makeText(this, "Authentication error. Please log in.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch reviews for the user who posted the ad (ad.userId)
                val response = apiService.getUserReviews("Bearer $token", ad.userId)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        val reviews = response.body()!!
                        reviewAdapter.updateReviews(reviews)
                        if (reviews.isEmpty()) {
                            reviewsRecyclerView.visibility = View.GONE
                            noReviewsTextView.visibility = View.VISIBLE
                        } else {
                            reviewsRecyclerView.visibility = View.VISIBLE
                            noReviewsTextView.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(this@ReviewActivity, "Failed to load reviews.", Toast.LENGTH_SHORT).show()
                        Log.e("ReviewActivity", "Failed to load reviews: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@ReviewActivity, "Error fetching reviews: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ReviewActivity", "Exception fetching reviews", e)
                }
            }
        }
    }

    private fun postUserReview() {
        val rating = ratingBar.rating
        val reviewText = reviewEditText.text.toString().trim()

        if (rating == 0f) {
            Toast.makeText(this, "Please select a star rating.", Toast.LENGTH_SHORT).show()
            return
        }

        if (reviewText.isEmpty()) {
            Toast.makeText(this, "Please write a review.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!loadingDialog.isAdded) {
            loadingDialog.show(supportFragmentManager, "loading")
        }

        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            loadingDialog.dismiss()
            Toast.makeText(this, "Authentication error. Please log in.", Toast.LENGTH_SHORT).show()
            return
        }

        val reviewRequest = ReviewRequest(
            advert_id = ad.id ?: 0, // Assuming ad.id is available and an Int. Use 0 as fallback.
            user_id = ad.userId, // The ID of the user being reviewed (the seller)
            rating = rating.toInt(),
            review_text = reviewText
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.postReview("Bearer $token", reviewRequest)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@ReviewActivity, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        // Clear the input fields
                        ratingBar.rating = 0f
                        reviewEditText.setText("")
                        // Refresh the list of reviews
                        fetchReviews()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@ReviewActivity, "Failed to submit review. $errorBody", Toast.LENGTH_LONG).show()
                        Log.e("ReviewActivity", "Failed to submit review: ${response.code()} - $errorBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@ReviewActivity, "Error submitting review: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ReviewActivity", "Exception submitting review", e)
                }
            }
        }
    }
}
