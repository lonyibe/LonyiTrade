package com.lonyitrade.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Review(
    val id: Int,
    val advert_id: Int,
    val user_id: String,
    val rating: Int,
    val review_text: String?,
    val created_at: String, // Keep as String for simplicity, format on client-side if needed
    val reviewer_name: String?, // Added for displaying user's name
    val reviewer_photo_url: String? // Added for displaying user's profile picture
) : Parcelable