package com.lonyitrade.app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lonyitrade.app.data.models.Ad

class SharedViewModel : ViewModel() {
    private val _adList = MutableLiveData<MutableList<Ad>>(mutableListOf())
    val adList: LiveData<MutableList<Ad>> = _adList

    fun setAdList(adList: MutableList<Ad>) {
        _adList.value = adList
    }
}