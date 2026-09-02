package com.xxy.activitystack.data

import android.content.Context
import androidx.core.content.edit

object Settings {

    private const val FILE = "activity_stack"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL = "interval_ms"
    private const val KEY_SHOW_PACKAGE = "show_package"
    private const val KEY_OVERLAY_X = "overlay_x"
    private const val KEY_OVERLAY_Y = "overlay_y"
    private const val KEY_OVERLAY_SCALE = "overlay_scale"

    val INTERVAL_OPTIONS = longArrayOf(300L, 500L, 1000L, 2000L)

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    var intervalMs: Long
        get() = prefs.getLong(KEY_INTERVAL, 500L)
        set(value) = prefs.edit { putLong(KEY_INTERVAL, value) }

    var showPackage: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PACKAGE, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_PACKAGE, value) }

    var overlayX: Int
        get() = prefs.getInt(KEY_OVERLAY_X, Int.MIN_VALUE)
        set(value) = prefs.edit { putInt(KEY_OVERLAY_X, value) }

    var overlayY: Int
        get() = prefs.getInt(KEY_OVERLAY_Y, 120)
        set(value) = prefs.edit { putInt(KEY_OVERLAY_Y, value) }

    var overlayScale: Float
        get() = prefs.getFloat(KEY_OVERLAY_SCALE, 1f)
        set(value) = prefs.edit { putFloat(KEY_OVERLAY_SCALE, value.coerceIn(0.6f, 3f)) }
}
