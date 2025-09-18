package com.lonyitrade.app.data.models

data class Ad(
    val type: String, // "sell" or "buy"
    val title: String,
    val description: String,
    val price: String?, // Nullable for "buy" requests
    val budget: String?, // Nullable for "sell" ads
    val condition: String,
    val district: String,
    val contact: String?, // Nullable for sell ads using registered info
    val showOnWhatsapp: Boolean?, // Nullable for sell ads
    val photos: List<String> // list of image URIs
)