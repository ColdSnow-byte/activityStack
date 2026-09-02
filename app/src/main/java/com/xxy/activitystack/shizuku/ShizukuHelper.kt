package com.xxy.activitystack.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * Shizuku 连接状态与授权管理。
 *
 * 仅使用 Shizuku 官方 README 中稳定提供的 API，避免版本差异导致的编译问题。
 */
object ShizukuHelper {

    /** 业务自定义的授权请求码 */
    const val REQUEST_CODE = 10086

    private val _binderAlive = MutableStateFlow(false)
    val binderAlive: StateFlow<Boolean> = _binderAlive.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private var attached = false

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        refresh()
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refresh()
    }

    /** 在 Application 中调用一次 */
    fun attach() {
        if (attached) return
        attached = true
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }
        refresh()
    }

    /** Shizuku 服务已连接 */
    fun isBinderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    /** Shizuku 服务已连接且已授予授权 */
    fun hasPermission(): Boolean = isBinderAlive() && try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    /** 请求 Shizuku 授权（需确保 Shizuku 已激活） */
    fun requestPermission() {
        if (isBinderAlive()) {
            runCatching { Shizuku.requestPermission(REQUEST_CODE) }
        }
    }

    fun refresh() {
        _binderAlive.value = isBinderAlive()
        _permissionGranted.value = hasPermission()
    }
}
