package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.LoginRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        // Fix: Changed variable name to phoneNumberEditText to match backend expectations
        val phoneNumberEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupTextView = findViewById<TextView>(R.id.signupTextView)

        loginButton.setOnClickListener {
            // Fix: Use the value from the correct EditText, which is named emailEditText but should contain a phone number
            val phoneNumber = phoneNumberEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (phoneNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = LoginRequest(phoneNumber, password)
                        val response = ApiClient.apiService.loginUser(request)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                val token = response.body()?.token
                                if (token != null) {
                                    // Save the token to maintain the session
                                    sessionManager.saveAuthToken(token)
                                    sessionManager.saveUserData("user_name_here", phoneNumber) // You might need to adjust this part

                                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@LoginActivity, MainAppActivity::class.java)
                                    startActivity(intent)
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
                            Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        signupTextView.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
}