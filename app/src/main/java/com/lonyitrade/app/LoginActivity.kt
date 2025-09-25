package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.LoginRequest
import com.lonyitrade.app.data.models.TokenResponse
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var loginButton: Button
    private lateinit var loginProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        val phoneNumberEditText = findViewById<EditText>(R.id.phoneNumberEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        loginProgressBar = findViewById(R.id.loginProgressBar)
        val signupTextView = findViewById<TextView>(R.id.signupTextView)

        loginButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (phoneNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                showLoading(true)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = LoginRequest(phoneNumber, password)
                        val response = ApiClient.apiService.loginUser(request)

                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            if (response.isSuccessful) {
                                val tokenResponse = response.body()
                                if (tokenResponse != null) {
                                    sessionManager.saveAuthToken(tokenResponse.token)
                                    sessionManager.saveUserData("user_name_here", phoneNumber)

                                    // NEW: Save user ID from the token
                                    val userId = getUserIdFromToken(tokenResponse.token)
                                    userId?.let { sessionManager.saveUserId(it) }

                                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@LoginActivity, MainAppActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this@LoginActivity, "Login failed: Token not received", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this@LoginActivity, "Login failed: Invalid credentials", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        signupTextView.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loginButton.text = ""
            loginProgressBar.visibility = View.VISIBLE
            loginButton.isEnabled = false
        } else {
            loginButton.text = "Login"
            loginProgressBar.visibility = View.GONE
            loginButton.isEnabled = true
        }
    }

    // NEW: Helper function to get the user ID from the JWT token payload
    private fun getUserIdFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val tokenPayload = Gson().fromJson(payload, Map::class.java)
            tokenPayload["userId"] as String
        } catch (e: Exception) {
            null
        }
    }
}