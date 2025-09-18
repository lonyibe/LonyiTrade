package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.RegisterRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {

    private lateinit var profilePicImageView: ImageView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            profilePicImageView.setImageURI(uri)
            Toast.makeText(this, "Photo uploaded!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        profilePicImageView = findViewById(R.id.profilePicImageView)
        val uploadPicButton = findViewById<Button>(R.id.uploadPicButton)
        val loginTextView = findViewById<TextView>(R.id.loginTextView)
        val signupButton = findViewById<Button>(R.id.signupButton)

        // Corrected variable names to match the XML IDs
        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val phoneNumberEditText = findViewById<EditText>(R.id.phoneNumberEditText)
        val districtEditText = findViewById<EditText>(R.id.districtEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        // Removed findViewById for 'confirmPasswordEditText' as it does not exist in the layout

        uploadPicButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        loginTextView.setOnClickListener {
            // Updated to reference LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        signupButton.setOnClickListener {
            val fullName = nameEditText.text.toString()
            val phoneNumber = phoneNumberEditText.text.toString()
            val district = districtEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (fullName.isEmpty() || phoneNumber.isEmpty() || district.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = RegisterRequest(fullName, phoneNumber, password, district)
                        val response = ApiClient.apiService.registerUser(request)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@SignupActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@SignupActivity, MainAppActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this@SignupActivity, "Registration failed: User may already exist", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SignupActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}