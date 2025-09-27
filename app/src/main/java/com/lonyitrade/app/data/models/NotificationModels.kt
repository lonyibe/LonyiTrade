// lonyibe/lonyitrade/LonyiTrade-a40125be66066d5d054a644e2d30a75cbee1ec54/app/src/main/java/com/lonyitrade/app/data/models/NotificationModels.kt
package com.lonyitrade.app.data.models

import com.lonyitrade.app.data.models.Message
import com.lonyitrade.app.data.models.UserProfile // Assuming UserProfile is the correct type

// FIX 1: New Data Class for the combined notification counts API response
data class NotificationCountsResponse(
    val unreadMessageCount: Int,
    val unreadReviewCount: Int
)

// The ReviewNotification data class (used in ApiService) must also be defined here or elsewhere.
// Assuming the simplest structure based on context:
data class ReviewNotification(
    val id: String,
    val reviewer: UserProfile,
    val reviewText: String,
    val rating: Int,
    val timestamp: Long,
    val isRead: Boolean = false
)

// A simplified ConversationSummary to satisfy dependencies (if not already defined)
data class ConversationSummary(
    val advertId: String,
    val otherUser: UserProfile,
    val latestMessage: Message,
    val unreadCount: Int
)