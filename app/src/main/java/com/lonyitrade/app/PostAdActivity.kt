package com.lonyitrade.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.lonyitrade.app.utils.SessionManager

class PostAdActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val selectedPhotos = mutableListOf<ImageView>()

    // This launcher handles multiple image selection from the gallery
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            // Display the selected images in the ImageViews
            for ((index, uri) in uris.withIndex()) {
                if (index < selectedPhotos.size) {
                    selectedPhotos[index].setImageURI(uri)
                }
            }
            Toast.makeText(this, "Photos uploaded!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_ad)

        sessionManager = SessionManager(this)

        val titleEditText = findViewById<EditText>(R.id.titleEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val priceEditText = findViewById<EditText>(R.id.priceEditText)
        val priceTypeRadioGroup = findViewById<RadioGroup>(R.id.priceTypeRadioGroup)

        val contactNameEditText = findViewById<EditText>(R.id.contactNameEditText)
        val contactPhoneEditText = findViewById<EditText>(R.id.contactPhoneEditText)
        val whatsappSwitch = findViewById<Switch>(R.id.whatsappSwitch)
        val conditionRadioGroup = findViewById<RadioGroup>(R.id.conditionRadioGroup)
        val postAdButton = findViewById<Button>(R.id.postAdButton)

        // Find the ImageViews for photos and add them to a list
        val photo1ImageView = findViewById<ImageView>(R.id.photo1ImageView)
        val photo2ImageView = findViewById<ImageView>(R.id.photo2ImageView)
        val photo3ImageView = findViewById<ImageView>(R.id.photo3ImageView)
        val photo4ImageView = findViewById<ImageView>(R.id.photo4ImageView)

        selectedPhotos.add(photo1ImageView)
        selectedPhotos.add(photo2ImageView)
        selectedPhotos.add(photo3ImageView)
        selectedPhotos.add(photo4ImageView)

        // Pre-fill user data
        contactNameEditText.setText(sessionManager.getFullName() ?: "")
        contactPhoneEditText.setText(sessionManager.getPhoneNumber() ?: "")

        // Handle photo upload button click by adding a listener to the ImageView
        photo1ImageView.setOnClickListener {
            pickImages.launch("image/*")
        }

        // Handle Post Ad button click (placeholder for backend logic)
        postAdButton.setOnClickListener {
            Toast.makeText(this, "Ad Posted! (Backend logic to be implemented)", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}