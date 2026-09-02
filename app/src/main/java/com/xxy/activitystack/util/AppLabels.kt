package com.xxy.activitystack.util

import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.ConcurrentHashMap

object AppLabels {

    private val cache = ConcurrentHashMap<String, String?>()

    fun of(context: Context, packageName: String): String? = cache[packageName] ?: run {
        val label = try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
        cache[packageName] = label
        label
    }
}

object Permissions {
    fun overlayGranted(context: Context): Boolean = android.provider.Settings.canDrawOverlays(context)
}
