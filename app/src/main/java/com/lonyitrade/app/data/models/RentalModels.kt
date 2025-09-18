package com.lonyitrade.app.data.models

import com.google.gson.annotations.SerializedName

// Model for rental data received from the server
data class Rental(
    val id: String?,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("property_type") val propertyType: String?,
    val city: String?,
    val district: String?,
    val rooms: Int?,
    @SerializedName("location_description") val locationDescription: String?,
    @SerializedName("monthly_rent") val monthlyRent: Double?,
    @SerializedName("price_type") val priceType: String?,
    val rules: String?,
    val description: String?,
    @SerializedName("landlord_name") val landlordName: String?,
    @SerializedName("landlord_phone") val landlordPhone: String?,
    @SerializedName("landlord_email") val landlordEmail: String?,
    @SerializedName("landlord_whatsapp") val landlordWhatsapp: String?,
    @SerializedName("landlord_type") val landlordType: String?,
    val photos: List<String>?
)

// Model for sending new rental data to the server
data class RentalRequest(
    @SerializedName("property_type") val propertyType: String,
    val city: String,
    val district: String,
    val rooms: Int,
    @SerializedName("location_description") val locationDescription: String,
    @SerializedName("monthly_rent") val monthlyRent: Double,
    @SerializedName("price_type") val priceType: String,
    val rules: String,
    val description: String,
    @SerializedName("landlord_name") val landlordName: String,
    @SerializedName("landlord_phone") val landlordPhone: String,
    @SerializedName("landlord_email") val landlordEmail: String,
    @SerializedName("landlord_whatsapp") val landlordWhatsapp: String?,
    @SerializedName("landlord_type") val landlordType: String
)