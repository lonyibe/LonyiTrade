package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.AdAdapter
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyAdsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ads)

        val myAdsRecyclerView = findViewById<RecyclerView>(R.id.myAdsRecyclerView)
        val noAdsTextView = findViewById<TextView>(R.id.noAdsTextView)
        myAdsRecyclerView.layoutManager = LinearLayoutManager(this)

        val sessionManager = SessionManager(this)
        val token = sessionManager.fetchAuthToken()

        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.apiService.getMyAdverts("Bearer $token")
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val ads = response.body() ?: emptyList()
                            if (ads.isEmpty()) {
                                myAdsRecyclerView.visibility = View.GONE
                                noAdsTextView.visibility = View.VISIBLE
                            } else {
                                myAdsRecyclerView.visibility = View.VISIBLE
                                noAdsTextView.visibility = View.GONE
                                myAdsRecyclerView.adapter = AdAdapter(ads)
                            }
                        } else {
                            Toast.makeText(this@MyAdsActivity, "Failed to load ads", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    // Handle error, e.g., show a toast
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MyAdsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}