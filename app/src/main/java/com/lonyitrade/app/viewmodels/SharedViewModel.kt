package com.lonyitrade.app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MediatorLiveData
import com.lonyitrade.app.data.models.Ad

class SharedViewModel : ViewModel() {
    // For signaling fragments to refresh ad data
    private val _adRefreshRequired = MutableLiveData<Boolean>()
    val adRefreshRequired: LiveData<Boolean> = _adRefreshRequired

    // For signaling fragments to refresh rental data
    private val _rentalRefreshRequired = MutableLiveData<Boolean>()
    val rentalRefreshRequired: LiveData<Boolean> = _rentalRefreshRequired

    // For holding the unread message count
    private val _unreadMessageCount = MutableLiveData<Int>(0)
    val unreadMessageCount: LiveData<Int> = _unreadMessageCount

    // FIX 1: New LiveData for unread review count
    private val _unreadReviewCount = MutableLiveData<Int>(0)
    val unreadReviewCount: LiveData<Int> = _unreadReviewCount

    // FIX 2: MediatorLiveData to combine message and review counts
    private val _totalNotificationCount = MediatorLiveData<Int>()
    val totalNotificationCount: LiveData<Int> = _totalNotificationCount

    // For holding the list of ads for HomeFragment
    private val _adList = MutableLiveData<List<Ad>>()
    val adList: LiveData<List<Ad>> = _adList

    init {
        // Initialize the MediatorLiveData to react to changes in either count
        _totalNotificationCount.addSource(_unreadMessageCount) {
            updateTotal()
        }
        _totalNotificationCount.addSource(_unreadReviewCount) {
            updateTotal()
        }
    }

    private fun updateTotal() {
        val messageCount = _unreadMessageCount.value ?: 0
        val reviewCount = _unreadReviewCount.value ?: 0
        _totalNotificationCount.value = messageCount + reviewCount
    }

    fun requireAdRefresh() {
        _adRefreshRequired.value = true
    }

    fun onAdRefreshDone() {
        _adRefreshRequired.value = false
    }

    fun requireRentalRefresh() {
        _rentalRefreshRequired.value = true
    }

    fun onRentalRefreshDone() {
        _rentalRefreshRequired.value = false
    }

    fun setUnreadMessageCount(count: Int) {
        // Use postValue if this might be called from a background thread (like a WebSocket listener)
        _unreadMessageCount.postValue(count)
    }

    // FIX 3: New function to update *both* counts from initial API fetch or WebSocket
    fun updateTotalNotificationCount(messageCount: Int, reviewCount: Int) {
        _unreadMessageCount.postValue(messageCount)
        _unreadReviewCount.postValue(reviewCount)
        // updateTotal() will be automatically called via MediatorLiveData observers
    }

    // New function to handle unread review count updates (e.g., from new WebSocket messages)
    fun setUnreadReviewCount(count: Int) {
        _unreadReviewCount.postValue(count)
    }

    fun setAdList(ads: List<Ad>) {
        _adList.postValue(ads)
    }
}