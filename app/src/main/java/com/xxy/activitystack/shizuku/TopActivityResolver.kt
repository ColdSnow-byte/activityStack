package com.xxy.activitystack.shizuku

import android.content.ComponentName
import android.os.Build
import android.os.IBinder
import android.util.Log
import moe.shizuku.server.IShizukuService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.io.FileInputStream

/**
 * 通过 Shizuku（shell / root 权限）实时读取当前前台的 Activity。
 *
 * 优先使用 `IActivityTaskManager.getFocusedRootTaskInfo()`，失败后依次降级到
 * `getTasks()` 与 `dumpsys activity activities`，以适配不同 Android 版本 / ROM。
 */
object TopActivityResolver {

    private const val TAG = "TopActivityResolver"
    private const val SERVICE_NAME = "activity_task"

    /**
     * 匹配 dumpsys 中的 resumedActivity，例如：
     * `topResumedActivity=ActivityRecord{abc u0 com.android.settings/.Settings t123}`
     * `mResumedActivity: ActivityRecord{abc u0 com.android.settings/.Settings t123}`
     */
    private val RESUMED_REGEX = Regex(
        "(?:topResumedActivity|mResumedActivity|resumedActivity)\\s*[:=]\\s*" +
            "(?:ActivityRecord\\{[^}]*?\\s+)?" +
            "(?:u\\d+\\s+)?" +
            "([A-Za-z0-9_.]+)/([A-Za-z0-9_.$]+)"
    )

    @Volatile
    private var cachedProxy: Any? = null

    @Volatile
    private var hiddenApiReady = false

    @Volatile
    private var binderUsable = true

    /**
     * 返回当前前台 Activity 的 [ComponentName]，获取失败返回 null。
     * 必须在非主线程调用。
     */
    fun resolve(): ComponentName? {
        if (binderUsable) {
            val viaBinder = runCatching { resolveViaBinder() }.getOrNull()
            if (viaBinder != null) return viaBinder
            Log.w(TAG, "IActivityTaskManager 不可用，降级到 dumpsys")
            binderUsable = false
            cachedProxy = null
        }
        return runCatching { resolveViaDumpsys() }.getOrNull()
    }

    private fun resolveViaBinder(): ComponentName? {
        val proxy = activityTaskManager() ?: return null
        val iface = Class.forName("android.app.IActivityTaskManager")

        runCatching {
            val info = iface.getDeclaredMethod("getFocusedRootTaskInfo").invoke(proxy)
            readComponent(info, "topActivity")?.let { return it }
        }

        runCatching {
            val tasks = iface.getDeclaredMethod("getTasks", Int::class.javaPrimitiveType)
                .invoke(proxy, 1) as? List<*>
            readComponent(tasks?.firstOrNull(), "topActivity")?.let { return it }
        }
        return null
    }

    private fun activityTaskManager(): Any? {
        cachedProxy?.let { return it }
        prepareHiddenApi()

        val rawBinder = Class.forName("android.os.ServiceManager")
            .getDeclaredMethod("getService", String::class.java)
            .invoke(null, SERVICE_NAME) as? IBinder ?: return null

        val stub = Class.forName("android.app.IActivityTaskManager\$Stub")
        val proxy = stub.getDeclaredMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(rawBinder)) ?: return null

        cachedProxy = proxy
        return proxy
    }

    private fun prepareHiddenApi() {
        if (hiddenApiReady) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L")
        }
        hiddenApiReady = true
    }

    private fun readComponent(host: Any?, fieldName: String): ComponentName? {
        if (host == null) return null
        val field = host.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }
        return field.get(host) as? ComponentName
    }

    /** 通过 Shizuku 以 shell 身份执行 `dumpsys activity activities` 并解析结果 */
    private fun resolveViaDumpsys(): ComponentName? {
        val binder = runCatching { Shizuku.getBinder() }.getOrNull() ?: return null
        val service = IShizukuService.Stub.asInterface(binder)
        val process = service.newProcess(
            arrayOf("sh", "-c", "dumpsys activity activities 2>/dev/null"),
            null,
            null
        ) ?: return null

        return try {
            val output = process.inputStream.use { fd ->
                FileInputStream(fd.fileDescriptor).bufferedReader().readText()
            }
            val match = RESUMED_REGEX.find(output) ?: return null
            val pkg = match.groupValues[1]
            val cls = match.groupValues[2]
            val fullClass = if (cls.startsWith(".")) pkg + cls else cls
            ComponentName(pkg, fullClass)
        } finally {
            runCatching { process.destroy() }
        }
    }
}
