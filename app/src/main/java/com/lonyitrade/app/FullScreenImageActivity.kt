package com.lonyitrade.app

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.lonyitrade.app.adapters.AdPhotoAdapter
import com.lonyitrade.app.api.ApiClient

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val viewPager: ViewPager2 = findViewById(R.id.fullScreenViewPager)
        val backButton: ImageView = findViewById(R.id.backButton)

        val imageUrls = intent.getStringArrayListExtra("image_urls")
        val position = intent.getIntExtra("position", 0)

        if (imageUrls != null) {
            val adapter = AdPhotoAdapter(imageUrls)
            viewPager.adapter = adapter
            viewPager.setCurrentItem(position, false)
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
