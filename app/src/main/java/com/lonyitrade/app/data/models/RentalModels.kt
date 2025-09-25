package com.lonyitrade.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Model for rental data received from the server
@Parcelize
data class Rental(
    val id: String?,
    val user_id: String?,
    val property_type: String?,
    val city: String?,
    val district: String?,
    val rooms: Int?,
    val location_description: String?,
    val monthly_rent: Double?,
    val price_type: String?,
    val rules: String?,
    val description: String?,
    val landlord_name: String?,
    val landlord_phone: String?,
    val landlord_email: String?,
    val landlord_whatsapp: String?,
    val landlord_type: String?,
    val photos: List<String>?
) : Parcelable

// Model for sending new rental data to the server
data class RentalRequest(
    val property_type: String,
    val city: String,
    val district: String,
    val rooms: Int,
    val location_description: String,
    val monthly_rent: Double,
    val price_type: String,
    val rules: String,
    val description: String,
    val landlord_name: String,
    val landlord_phone: String,
    val landlord_email: String,
    val landlord_whatsapp: String?,
    val landlord_type: String
)