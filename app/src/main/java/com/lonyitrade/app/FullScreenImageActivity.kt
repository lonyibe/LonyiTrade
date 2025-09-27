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

        // This activity should typically run edge-to-edge.
        // If the theme in AndroidManifest is not already NoActionBar or FullScreen,
        // you might need to apply system UI flags here.
        // However, relying on the XML margin fix for the back button is simpler
        // and avoids the 'Unresolved reference: Theme_LonyiTrade_NoActionBar' error.

        setContentView(R.layout.activity_full_screen_image)

        val viewPager: ViewPager2 = findViewById(R.id.fullScreenViewPager)
        val backButton: ImageView = findViewById(R.id.backButton)

        // FIX: Removed the complex and error-prone setOnApplyWindowInsetsListener logic
        // The overlap is now fixed by the 48dp top margin applied in the XML.

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