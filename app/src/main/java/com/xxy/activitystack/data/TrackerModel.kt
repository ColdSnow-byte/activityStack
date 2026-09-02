package com.xxy.activitystack.data

enum class TrackerState {
    IDLE,
    NO_SHIZUKU,
    NO_PERMISSION,
    RUNNING,
    ERROR,
}

data class TrackerStatus(
    val state: TrackerState = TrackerState.IDLE,
    val detail: String = "",
)

data class ForegroundActivity(
    val packageName: String,
    val className: String,
    val appLabel: String?,
    val updatedAt: Long,
) {
    /** 去掉包名前缀后的短类名，例如 `.Settings` */
    val shortClassName: String
        get() = if (className.startsWith(packageName) && className.length > packageName.length) {
            className.substring(packageName.length)
        } else {
            className
        }

    /** 完整的 `包名/类名` */
    val flatten: String
        get() = "$packageName/$className"
}
