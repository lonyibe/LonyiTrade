package com.lonyitrade.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.lonyitrade.app.data.models.LoginRequest
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
                        val response = ApiClient.apiService.registerUser(request)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                val userProfile = response.body()
                                if (userProfile != null) {
                                    // Automatically log in to get a token
                                    loginAndProceed(userProfile.id!!, phoneNumber, password)
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

    private fun loginAndProceed(userId: String, phoneNumber: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loginRequest = LoginRequest(phoneNumber, password)
                val loginResponse = ApiClient.apiService.loginUser(loginRequest)

                withContext(Dispatchers.Main) {
                    if (loginResponse.isSuccessful) {
                        val token = loginResponse.body()?.token
                        if (token != null) {
                            sessionManager.saveAuthToken(token)
                            if (profilePictureUri != null) {
                                uploadProfilePicture(userId, profilePictureUri!!)
                            } else {
                                Toast.makeText(this@SignupActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                navigateToMainApp()
                            }
                        } else {
                            showLoading(false)
                            Toast.makeText(this@SignupActivity, "Login after signup failed.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        showLoading(false)
                        Toast.makeText(this@SignupActivity, "Login after signup failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@SignupActivity, "Network error during login: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun uploadProfilePicture(userId: String, photoUri: Uri) {
        getTempFileFromUri(photoUri)?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)
            val token = sessionManager.fetchAuthToken()

            if (token == null) {
                showLoading(false)
                Toast.makeText(this@SignupActivity, "Authentication error. Please log in.", Toast.LENGTH_LONG).show()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ApiClient.apiService.uploadProfilePicture("Bearer $token", userId, photoPart)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SignupActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        navigateToMainApp()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SignupActivity, "Photo upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        navigateToMainApp()
                    }
                }
            }
        }
    }

    private fun navigateToMainApp() {
        val intent = Intent(this@SignupActivity, MainAppActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
