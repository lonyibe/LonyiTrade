package com.lonyitrade.app.data.models

import com.google.gson.annotations.SerializedName

data class JobApplication(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("job_ad_id") val jobAdId: String?,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("job_interested_in") val jobInterestedIn: String,
    @SerializedName("education_level") val educationLevel: String,
    @SerializedName("created_at") val createdAt: String,
    val user: UserProfile?,
    val jobAd: JobAd?
)

data class JobApplicationRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("job_interested_in") val jobInterestedIn: String,
    @SerializedName("education_level") val educationLevel: String
)

data class JobAd(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("created_at") val createdAt: String
)
