package com.xxy.activitystack.data

import android.content.Context
import com.xxy.activitystack.accessibility.AccessibilityTracker
import com.xxy.activitystack.shizuku.ShizukuHelper
import com.xxy.activitystack.shizuku.TopActivityResolver
import com.xxy.activitystack.util.AppLabels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 前台 Activity 轮询器。进程内单例，界面与悬浮窗共用同一份状态。
 */
object ActivityTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _info = MutableStateFlow<ForegroundActivity?>(null)
    val info: StateFlow<ForegroundActivity?> = _info.asStateFlow()

    private val _status = MutableStateFlow(TrackerStatus())
    val status: StateFlow<TrackerStatus> = _status.asStateFlow()

    private lateinit var appContext: Context
    private var job: Job? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val running: Boolean
        get() = job?.isActive == true

    fun start() {
        if (job?.isActive == true) return
        // 优先使用无障碍服务作为事件驱动来源，无需轮询
        if (AccessibilityTracker.isActive) {
            _status.value = TrackerStatus(TrackerState.RUNNING, "")
            return
        }
        job = scope.launch {
            while (isActive) {
                tick()
                delay(Settings.intervalMs.coerceIn(200L, 5000L))
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _status.value = TrackerStatus(TrackerState.IDLE)
    }

    private suspend fun tick() {
        if (!ShizukuHelper.isBinderAlive()) {
            _status.value = TrackerStatus(TrackerState.NO_SHIZUKU, "Shizuku 未运行或未激活")
            return
        }
        if (!ShizukuHelper.hasPermission()) {
            _status.value = TrackerStatus(TrackerState.NO_PERMISSION, "尚未授予 Shizuku 授权")
            return
        }

        val component = withTimeoutOrNull(3_000) { TopActivityResolver.resolve() }
        if (component == null) {
            _status.value = TrackerStatus(TrackerState.ERROR, "读取前台 Activity 失败")
            return
        }

        val now = System.currentTimeMillis()
        _info.value = ForegroundActivity(
            packageName = component.packageName,
            className = component.className,
            appLabel = AppLabels.of(appContext, component.packageName),
            updatedAt = now,
        )
        _status.value = TrackerStatus(TrackerState.RUNNING, "")
    }

    /**
     * 由无障碍服务在窗口切换时调用，推送当前前台 Activity。
     */
    fun updateFromAccessibility(packageName: String, className: String) {
        val now = System.currentTimeMillis()
        _info.value = ForegroundActivity(
            packageName = packageName,
            className = className,
            appLabel = AppLabels.of(appContext, packageName),
            updatedAt = now,
        )
        _status.value = TrackerStatus(TrackerState.RUNNING, "")
    }

    fun formatTime(timeMillis: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timeMillis))
}
