package com.lonyitrade.app.data.models

import com.google.gson.annotations.SerializedName

// Model for user registration data
data class RegisterRequest(
    val fullName: String,
    val phoneNumber: String,
    val password: String,
    val district: String
)

// Model for user login data
data class LoginRequest(
    val phoneNumber: String,
    val password: String
)

// Model for the response from the /register endpoint
data class AuthResponse(
    val id: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    val district: String,
    val token: String // New field added for the JWT token
)

// Model for the response from the /login endpoint
data class TokenResponse(
    val token: String
)