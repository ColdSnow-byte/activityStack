package com.xxy.activitystack.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.xxy.activitystack.data.ForegroundActivity
import com.xxy.activitystack.data.Settings
import com.xxy.activitystack.ui.overlay.FloatingOverlay
import com.xxy.activitystack.ui.theme.ActivityStackTheme
import com.xxy.activitystack.util.Permissions
import kotlin.math.roundToInt

/**
 * 基于 WindowManager 的系统级悬浮窗，内部使用 Compose + Miuix 渲染。
 */
internal class FloatingWindow(
    private val service: TrackerService,
) {

    private val windowManager: WindowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val lifecycleOwner = OverlayLifecycleOwner()
    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    /** 当前缩放比例（0.6 ~ 3.0） */
    private val scaleState = mutableStateOf(Settings.overlayScale.coerceIn(0.6f, 3f))
    /** 面板原始（未缩放）尺寸，由 Compose 的 onGloballyPositioned 回传 */
    private var baseW = 0
    private var baseH = 0

    private fun onMeasure(w: Int, h: Int) {
        baseW = w
        baseH = h
        val p = params ?: return
        p.width = (w * scaleState.value).roundToInt().coerceAtMost(maxWindowW())
        p.height = (h * scaleState.value).roundToInt().coerceAtMost(maxWindowH())
        composeView?.post { runCatching { windowManager.updateViewLayout(composeView, p) } }
    }

    /** 窗口最大宽/高（px），不超过屏幕的 92%，避免大尺寸撑破屏幕 */
    private fun maxWindowW(): Int {
        val dm = service.resources.displayMetrics
        return ((dm.widthPixels - 24 * dm.density) * 0.92f).roundToInt().coerceAtLeast(160)
    }

    private fun maxWindowH(): Int {
        val dm = service.resources.displayMetrics
        return ((dm.heightPixels - 24 * dm.density) * 0.92f).roundToInt().coerceAtLeast(120)
    }

    fun show() {
        if (composeView != null) return
        if (!Permissions.overlayGranted(service)) return

        val view = ComposeView(service).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
        }

        val layoutParams = createLayoutParams()
        composeView = view
        params = layoutParams

        lifecycleOwner.onCreate()
        view.setContent {
            ActivityStackTheme {
                FloatingOverlay(
                    scale = scaleState.value,
                    onMeasure = ::onMeasure,
                    onDrag = ::drag,
                    onCopy = ::copyToClipboard,
                    onClose = { service.stopSelf() },
                )
            }
        }

        windowManager.addView(view, layoutParams)
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()
    }

    fun dismiss() {
        val view = composeView ?: return
        runCatching { windowManager.removeView(view) }
        composeView = null
        params = null
        lifecycleOwner.onPause()
        lifecycleOwner.onStop()
        lifecycleOwner.onDestroy()
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val savedX = Settings.overlayX
            val savedY = Settings.overlayY
            x = if (savedX == Int.MIN_VALUE) 24 else savedX
            y = if (savedX == Int.MIN_VALUE) 140 else savedY
        }
    }

    private fun drag(deltaX: Float, deltaY: Float) {
        val current = params ?: return
        current.x = (current.x + deltaX).roundToInt()
        current.y = (current.y + deltaY).roundToInt()
        val view = composeView ?: return
        runCatching { windowManager.updateViewLayout(view, current) }
        Settings.overlayX = current.x
        Settings.overlayY = current.y
    }

    private fun copyToClipboard(info: ForegroundActivity) {
        val manager = service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("activity", info.flatten))
        Toast.makeText(service, "已复制 ${info.flatten}", Toast.LENGTH_SHORT).show()
    }
}
