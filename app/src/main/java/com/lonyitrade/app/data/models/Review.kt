package com.lonyitrade.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Review(
    val id: String, // FIX: Changed from Int to String (UUID)
    val advert_id: String, // FIX: Changed from Int to String (UUID)
    val user_id: String,
    val reviewer_id: String,
    val rating: Int,
    val review_text: String?,
    val created_at: String,
    val reviewer_name: String?,
    val reviewer_photo_url: String?
) : Parcelable

data class ReviewRequest(
    val advert_id: String, // FIX: Changed from Int to String (UUID)
    val user_id: String,
    val rating: Int,
    val review_text: String?
)
