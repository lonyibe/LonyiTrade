package com.lonyitrade.app.data.models

import com.google.gson.annotations.SerializedName

// Model for the data received from the /api/adverts GET endpoint
data class Ad(
    val id: String?,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("advert_type") val type: String?,
    val title: String,
    val description: String,
    val category: String?,
    val price: String?,
    val district: String?,
    @SerializedName("created_at") val createdAt: String?,
    val photos: List<String>? // New field for image URLs
)

// Model for sending data to the /api/adverts POST endpoint
data class AdRequest(
    val title: String,
    val description: String,
    val category: String,
    @SerializedName("advert_type") val advertType: String,
    val price: Double,
    val district: String
)