package com.lonyitrade.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class MediaPreviewActivity : AppCompatActivity() {

    private lateinit var previewImageView: ImageView
    private lateinit var captionEditText: EditText
    private lateinit var sendMediaButton: Button
    private var mediaUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_preview)

        previewImageView = findViewById(R.id.previewImageView)
        captionEditText = findViewById(R.id.captionEditText)
        sendMediaButton = findViewById(R.id.sendMediaButton)

        mediaUri = intent.getParcelableExtra("mediaUri")

        if (mediaUri != null) {
            Glide.with(this)
                .load(mediaUri)
                .into(previewImageView)
        }

        sendMediaButton.setOnClickListener {
            val caption = captionEditText.text.toString()
            val resultIntent = Intent().apply {
                putExtra("mediaUri", mediaUri)
                putExtra("caption", caption)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}