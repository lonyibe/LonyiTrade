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
    val category: String,
    val price: String?,
    @SerializedName("price_type") val priceType: String?,
    val district: String?,
    val condition: String?,
    @SerializedName("seller_phone_number") val sellerPhoneNumber: String?,
    @SerializedName("created_at") val createdAt: String?,
    val photos: List<String>?
) : Parcelable

@Parcelize
data class AdRequest(
    val title: String,
    val description: String,
    val category: String,
    @SerializedName("advert_type") val advertType: String,
    val price: Double,
    @SerializedName("price_type") val priceType: String,
    val district: String,
    val condition: String
) : Parcelable

data class Message(
    val id: String?,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("advert_id") val advertId: String,
    val content: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("media_url") val mediaUrl: String?,
    @SerializedName("audio_url") val audioUrl: String?, // New field for audio messages
    val status: String?
)

@Parcelize
data class ConversationSummary(
    @SerializedName("advert_id") val advertId: String,
    @SerializedName("other_user_id") val otherUserId: String,
    @SerializedName("other_user_name") val otherUserName: String?,
    @SerializedName("other_user_photo_url") val otherUserPhotoUrl: String?,
    @SerializedName("advert_title") val advertTitle: String?,
    @SerializedName("advert_photos") val advertPhotos: List<String>?,
    @SerializedName("advert_price") val advertPrice: String?,
    @SerializedName("advert_price_type") val advertPriceType: String?,
    @SerializedName("last_message") var lastMessage: String?,
    @SerializedName("unread_count") var unreadCount: Int,
    @Transient var isNew: Boolean = false,
    @Transient var isTyping: Boolean = false
) : Parcelable
