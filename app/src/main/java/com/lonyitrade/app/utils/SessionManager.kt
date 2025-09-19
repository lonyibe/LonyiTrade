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
        const val LAST_USED_PHONE_NUMBER = "last_used_phone_number"
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(AUTH_TOKEN, token)
        editor.apply()
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(AUTH_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

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

    fun saveLastUsedPhoneNumber(phoneNumber: String) {
        val editor = prefs.edit()
        editor.putString(LAST_USED_PHONE_NUMBER, phoneNumber)
        editor.apply()
    }

    fun getLastUsedPhoneNumber(): String? {
        return prefs.getString(LAST_USED_PHONE_NUMBER, null)
    }

    fun logoutUser() {
        val editor = prefs.edit()
        editor.remove(AUTH_TOKEN)
        editor.remove(FULL_NAME)
        editor.remove(PHONE_NUMBER)
        editor.apply()
    }
}
