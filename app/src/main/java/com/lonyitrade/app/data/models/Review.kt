package com.lonyitrade.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Review(
    val id: String,
    val advert_id: String,
    val user_id: String,
    val reviewer_id: String,
    val rating: Int,
    val review_text: String?,
    val created_at: String,
    val reviewer_name: String?,
    val reviewer_photo_url: String?
) : Parcelable

data class ReviewRequest(
    val advert_id: String,
    val user_id: String,
    val rating: Int,
    val review_text: String?
)

// --- FIX: New data model for displaying reviews in the Notifications list ---
@Parcelize
data class ReviewNotification(
    val id: String,
    val advert_id: String,
    val user_id: String,
    val reviewer_id: String,
    val rating: Int,
    val review_text: String?,
    val created_at: String,
    val reviewer_name: String?,
    val reviewer_photo_url: String?,
    val advert_title: String? // CRITICAL: Used for context in the Notifications page
) : Parcelable