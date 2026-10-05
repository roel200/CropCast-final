package com.cropcast.app.data

import android.content.Context
import android.content.res.Configuration

/**
 * Language and light/dark theme are a choice of this phone, not of the farm, so they live here and not
 * in the Firebase settings: they also apply on the login screen, before anyone is signed in.
 */
class LocalPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("cropcast_ui", Context.MODE_PRIVATE)
    private val systemDark =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    /** Follows the phone's theme until the user picks one. */
    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK, systemDark)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_DARK = "dark"
        const val DEFAULT_LANGUAGE = "Filipino"
    }
}
