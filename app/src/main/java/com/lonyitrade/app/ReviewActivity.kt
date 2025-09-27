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
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.ReviewRequest
import com.lonyitrade.app.utils.LoadingDialogFragment
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewActivity : AppCompatActivity() {

    // FIX 1: Change to nullable var since ad might be loaded async via adId
    private var ad: Ad? = null
    private lateinit var backButton: ImageView
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var noReviewsTextView: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var reviewEditText: EditText
    private lateinit var submitReviewButton: Button
    private lateinit var submitReviewContainer: View
    private lateinit var reviewAdapter: ReviewAdapter

    private lateinit var sessionManager: SessionManager
    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(this) }
    private val loadingDialog = LoadingDialogFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        sessionManager = SessionManager(this)

        // 1. Check for parcelable Ad object (from AdDetailActivity)
        val adParcelable = intent.getParcelableExtra("AD_EXTRA") as? Ad
        // 2. Check for adId string (from Notification deep link via SplashActivity)
        val adIdFromNotification = intent.getStringExtra("adId")

        // FIX 2: Handle both normal flow (AD_EXTRA) and deep-link flow (adId)
        if (adParcelable != null) {
            ad = adParcelable
            initializeViews()
            setupReviewList()
            fetchReviews()
        } else if (adIdFromNotification != null) {
            // Case: Launched from notification (deep link)
            loadingDialog.show(supportFragmentManager, "loading")
            fetchAdDetails(adIdFromNotification)
        } else {
            // Case: No data at all
            Toast.makeText(this, "Error loading review context.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // FIX 3: New function to fetch Ad details by ID for notification deep-linking
    private fun fetchAdDetails(adId: String) {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            loadingDialog.dismiss()
            Toast.makeText(this, "Please log in to view this review.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use the API service to fetch the full Ad object
                val response = apiService.getAdvertById("Bearer $token", adId)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        ad = response.body() // Store the fetched Ad object
                        // Ad object is now ready, continue setup
                        ad?.let {
                            initializeViews()
                            setupReviewList()
                            fetchReviews()
                        }
                    } else {
                        Toast.makeText(this@ReviewActivity, "Failed to load ad details: ${response.code()}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@ReviewActivity, "Network error loading ad: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    // FIX 4: Update initializeViews to safely use the nullable 'ad'
    private fun initializeViews() {
        val currentAd = ad ?: return // Safety check, exit if ad is null/not yet loaded

        backButton = findViewById(R.id.backButton)
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView)
        noReviewsTextView = findViewById(R.id.noReviewsTextView)
        ratingBar = findViewById(R.id.ratingBar)
        reviewEditText = findViewById(R.id.reviewEditText)
        submitReviewButton = findViewById(R.id.submitReviewButton)
        submitReviewContainer = findViewById(R.id.submitReviewContainer)

        findViewById<TextView>(R.id.toolbarTitle).text = "Reviews for ${currentAd.title}"

        backButton.setOnClickListener {
            finish()
        }

        val currentUserId = sessionManager.getUserId()
        if (currentAd.userId == currentUserId) {
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

    // FIX 5: Update fetchReviews to safely use the nullable 'ad'
    private fun fetchReviews() {
        val currentAd = ad ?: return // Safety check, exit if ad is null

        if (!loadingDialog.isAdded) {
            loadingDialog.show(supportFragmentManager, "loading")
        }

        val token = sessionManager.fetchAuthToken()
        val userIdToReview = currentAd.userId // Use currentAd.userId

        if (token.isNullOrEmpty() || userIdToReview.isNullOrEmpty()) {
            loadingDialog.dismiss()
            Toast.makeText(this, "Authentication or User ID error. Please log in.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Correctly use the apiService instance
                val response = apiService.getUserReviews("Bearer $token", userIdToReview)
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
                        Toast.makeText(this@ReviewActivity, "Failed to load reviews: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                        Log.e("ReviewActivity", "Failed to load reviews: ${response.code()} - ${response.errorBody()?.string()}")
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

    // FIX 6: Update postUserReview to safely use the nullable 'ad'
    private fun postUserReview() {
        val currentAd = ad ?: return // Safety check, exit if ad is null

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

        val token = sessionManager.fetchAuthToken()
        val userIdToReview = currentAd.userId // Use currentAd.userId
        val advertId = currentAd.id // Use currentAd.id

        if (token.isNullOrEmpty() || userIdToReview.isNullOrEmpty() || advertId.isNullOrEmpty()) {
            Toast.makeText(this, "Review data missing. Cannot submit review.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!loadingDialog.isAdded) {
            loadingDialog.show(supportFragmentManager, "loading")
        }

        val reviewRequest = ReviewRequest(
            advert_id = advertId,
            user_id = userIdToReview,
            rating = rating.toInt(),
            review_text = reviewText
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Correctly use the apiService instance
                val response = apiService.postReview("Bearer $token", reviewRequest)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@ReviewActivity, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        ratingBar.rating = 0f
                        reviewEditText.setText("")
                        fetchReviews()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@ReviewActivity, "Failed to submit review. ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
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