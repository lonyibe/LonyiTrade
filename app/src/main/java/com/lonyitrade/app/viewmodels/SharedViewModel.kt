package com.lonyitrade.app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    // For holding the list of ads for HomeFragment
    private val _adList = MutableLiveData<List<Ad>>()
    val adList: LiveData<List<Ad>> = _adList

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

    fun setAdList(ads: List<Ad>) {
        _adList.postValue(ads)
    }
}

