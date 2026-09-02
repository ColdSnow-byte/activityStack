package com.xxy.activitystack.accessibility

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 无障碍服务状态管理。
 *
 * 与 Shizuku 平级，作为另一种「前台 Activity 来源」：当系统无障碍服务已启用并连接后，
 * 由 [ActivityStackAccessibilityService] 在窗口切换时把当前前台 Activity 推送给 [com.xxy.activitystack.data.ActivityTracker]。
 */
object AccessibilityTracker {

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    val isActive: Boolean
        get() = _active.value

    fun setActive(value: Boolean) {
        _active.value = value
    }

    /** 当前系统「已启用的无障碍服务」列表中是否包含本应用的服务 */
    fun isServiceEnabled(context: Context): Boolean {
        val component = context.packageName + "/" + ActivityStackAccessibilityService::class.qualifiedName
        val enabled = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(":").contains(component)
    }
}
