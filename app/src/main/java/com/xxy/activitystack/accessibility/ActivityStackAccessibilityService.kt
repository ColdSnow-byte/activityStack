package com.xxy.activitystack.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.xxy.activitystack.data.ActivityTracker

/**
 * 通过系统无障碍服务监听 `TYPE_WINDOW_STATE_CHANGED` 事件，实时获取当前前台 Activity。
 *
 * 该方式与 Shizuku 二选一即可驱动悬浮窗：启用本服务后，每当用户切换到不同界面，
 * 系统会回调 [onAccessibilityEvent]，从中取出包名与前台 Activity 类名。
 */
class ActivityStackAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityTracker.setActive(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        // 窗口状态变更的类名应为 Activity 类；过滤掉不含包名分隔符的异常事件
        if (!cls.contains(".")) return

        ActivityTracker.updateFromAccessibility(pkg, cls)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AccessibilityTracker.setActive(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AccessibilityTracker.setActive(false)
        super.onDestroy()
    }
}
