package com.lonyitrade.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.AuthResponse
import com.lonyitrade.app.data.models.RegisterRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SignupActivity : AppCompatActivity() {

    private lateinit var profilePicImageView: ImageView
    private lateinit var signupButton: Button
    private lateinit var signupProgressBar: ProgressBar
    private lateinit var sessionManager: SessionManager

    // Correctly initialize ApiClient
    private val apiService by lazy { ApiClient().getApiService(this) }

    private var profilePictureUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            profilePicImageView.setImageURI(uri)
            profilePictureUri = uri
            Toast.makeText(this, "Photo selected!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        sessionManager = SessionManager(this)

        profilePicImageView = findViewById(R.id.profilePicImageView)
        val uploadPicButton = findViewById<Button>(R.id.uploadPicButton)
        val loginTextView = findViewById<TextView>(R.id.loginTextView)
        signupButton = findViewById(R.id.signupButton)
        signupProgressBar = findViewById(R.id.signupProgressBar)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val phoneNumberEditText = findViewById<EditText>(R.id.phoneNumberEditText)
        val districtEditText = findViewById<EditText>(R.id.districtEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        uploadPicButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
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
                showLoading(true)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = RegisterRequest(fullName, phoneNumber, password, district)
                        // Correctly use the apiService instance
                        val response = apiService.registerUser(request)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                val authResponse = response.body()
                                if (authResponse != null) {
                                    sessionManager.saveAuthToken(authResponse.token)
                                    sessionManager.saveUserId(authResponse.id)

                                    if (profilePictureUri != null) {
                                        uploadProfilePicture(authResponse.id, profilePictureUri!!)
                                    } else {
                                        showLoading(false)
                                        Toast.makeText(this@SignupActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this@SignupActivity, MainAppActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    showLoading(false)
                                    Toast.makeText(this@SignupActivity, "Registration failed: Response body is null", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                showLoading(false)
                                Toast.makeText(this@SignupActivity, "Registration failed: User may already exist", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            Toast.makeText(this@SignupActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun uploadProfilePicture(userId: String, photoUri: Uri) {
        getTempFileFromUri(photoUri)?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)
            val token = sessionManager.fetchAuthToken()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (token != null) {
                        // Correctly use the apiService instance
                        apiService.uploadProfilePicture("Bearer $token", userId, photoPart)
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            Toast.makeText(this@SignupActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@SignupActivity, MainAppActivity::class.java))
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            Toast.makeText(this@SignupActivity, "Authentication error. Please log in.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(this@SignupActivity, "Photo upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        // Proceed to MainAppActivity even if photo upload fails
                        startActivity(Intent(this@SignupActivity, MainAppActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            signupButton.text = ""
            signupProgressBar.visibility = View.VISIBLE
            signupButton.isEnabled = false
        } else {
            signupButton.text = "Sign Up"
            signupProgressBar.visibility = View.GONE
            signupButton.isEnabled = true
        }
    }

    private fun getTempFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            file
        } catch (e: Exception) {
            null
        }
    }
}