package com.lonyitrade.app.data.models

import com.google.gson.annotations.SerializedName

data class UserProfile(
    val id: String?,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    val district: String?,
    @SerializedName("profile_picture_url") val profilePictureUrl: String?
)