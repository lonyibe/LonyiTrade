package com.lonyitrade.app

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.lonyitrade.app.api.ApiClient

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val fullScreenImageView: ImageView = findViewById(R.id.fullScreenImageView)
        val backButton: ImageView = findViewById(R.id.backButton)

        val imageUrl = intent.getStringExtra("IMAGE_URL")

        if (imageUrl != null) {
            val fullUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + imageUrl.trimStart('/')
            Glide.with(this)
                .load(fullUrl)
                .into(fullScreenImageView)
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}