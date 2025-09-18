package com.lonyitrade.app.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ad(
    val id: String?,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("advert_type") val type: String?,
    val title: String,
    val description: String,
    val category: String?,
    val price: String?,
    @SerializedName("price_type") val priceType: String?,
    val district: String?,
    val condition: String?,
    @SerializedName("seller_phone_number") val sellerPhoneNumber: String?,
    @SerializedName("created_at") val createdAt: String?,
    val photos: List<String>?
) : Parcelable

data class AdRequest(
    val title: String,
    val description: String,
    val category: String,
    @SerializedName("advert_type") val advertType: String,
    val price: Double,
    @SerializedName("price_type") val priceType: String,
    val district: String,
    val condition: String
)