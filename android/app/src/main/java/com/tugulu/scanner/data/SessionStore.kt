package com.tugulu.scanner.data

import android.content.Context
import com.tugulu.scanner.BuildConfig

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("tugulu_scanner", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, BuildConfig.DEFAULT_BASE_URL)?.trim().orEmpty()
        set(value) = prefs.edit().putString(KEY_BASE_URL, ApiClient.normalizeBaseUrl(value)).apply()

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var realName: String?
        get() = prefs.getString(KEY_REAL_NAME, null)
        set(value) = prefs.edit().putString(KEY_REAL_NAME, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var preferredCameraId: String?
        get() = prefs.getString(KEY_CAMERA_ID, null)
        set(value) = prefs.edit().putString(KEY_CAMERA_ID, value).apply()

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_REAL_NAME)
            .apply()
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_REAL_NAME = "real_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_CAMERA_ID = "camera_id"
    }
}
