package com.lonyitrade.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
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
    private lateinit var loginButton: Button
    private lateinit var loginProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        val phoneNumberEditText = findViewById<EditText>(R.id.emailEditText)
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
                                val token = response.body()?.token
                                if (token != null) {
                                    sessionManager.saveAuthToken(token)
                                    sessionManager.saveUserData("user_name_here", phoneNumber)
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
}