package com.lonyitrade.app.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private var prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "user_session"
        const val AUTH_TOKEN = "auth_token"
        const val FULL_NAME = "full_name"
        const val PHONE_NUMBER = "phone_number"
    }

    // This method saves the authentication token
    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(AUTH_TOKEN, token)
        editor.apply()
    }

    // This method retrieves the authentication token
    fun fetchAuthToken(): String? {
        return prefs.getString(AUTH_TOKEN, null)
    }

    // This method checks if the user is logged in
    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

    // You can also add methods to save and retrieve other user data
    fun saveUserData(fullName: String, phoneNumber: String) {
        val editor = prefs.edit()
        editor.putString(FULL_NAME, fullName)
        editor.putString(PHONE_NUMBER, phoneNumber)
        editor.apply()
    }

    fun getFullName(): String? {
        return prefs.getString(FULL_NAME, null)
    }

    fun getPhoneNumber(): String? {
        return prefs.getString(PHONE_NUMBER, null)
    }

    // This method logs the user out by clearing all saved preferences.
    fun logoutUser() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}