package com.lonyitrade.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetworkChangeReceiver(
    private val onNetworkAvailable: () -> Unit,
    private val onNetworkLost: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            if (NetworkUtils.isInternetAvailable(context)) {
                onNetworkAvailable.invoke()
            } else {
                onNetworkLost.invoke()
            }
        }
    }
}