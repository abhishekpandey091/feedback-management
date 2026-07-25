package com.example.feedbackmanagement

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "feedback_management_session",
        Context.MODE_PRIVATE
    )

    fun saveToken(token: String) {
        prefs.edit()
            .putString("token", token)
            .apply()
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun saveRole(role: String) {
        prefs.edit()
            .putString("role", role)
            .apply()
    }

    fun getRole(): String? {
        return prefs.getString("role", null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }
}