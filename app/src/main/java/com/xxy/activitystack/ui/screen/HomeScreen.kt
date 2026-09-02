package com.xxy.activitystack.ui.screen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings as SystemSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.xxy.activitystack.accessibility.AccessibilityTracker
import com.xxy.activitystack.data.ActivityTracker
import com.xxy.activitystack.data.ForegroundActivity
import com.xxy.activitystack.data.Settings
import com.xxy.activitystack.data.TrackerState
import com.xxy.activitystack.data.TrackerStatus
import com.xxy.activitystack.service.TrackerService
import com.xxy.activitystack.shizuku.ShizukuHelper
import com.xxy.activitystack.ui.theme.Palette
import com.xxy.activitystack.util.Permissions
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()

    val binderAlive by ShizukuHelper.binderAlive.collectAsState()
    val shizukuGranted by ShizukuHelper.permissionGranted.collectAsState()
    val a11yEnabled by AccessibilityTracker.active.collectAsState()
    val info by ActivityTracker.info.collectAsState()
    val status by ActivityTracker.status.collectAsState()

    var overlayGranted by remember { mutableStateOf(Permissions.overlayGranted(context)) }
    var notifyGranted by remember { mutableStateOf(notificationGranted(context)) }
    var enabled by remember { mutableStateOf(Settings.enabled) }
    var showPackage by remember { mutableStateOf(Settings.showPackage) }
    var interval by remember { mutableLongStateOf(Settings.intervalMs) }

    val notifyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifyGranted = notificationGranted(context) }

    LifecycleResumeEffect(Unit) {
        ShizukuHelper.refresh()
        overlayGranted = Permissions.overlayGranted(context)
        notifyGranted = notificationGranted(context)
        onPauseOrDispose { }
    }

    // 权限补齐后（例如刚授予悬浮窗 / 无障碍权限）重新拉起服务以显示悬浮窗
    LaunchedEffect(overlayGranted, shizukuGranted, a11yEnabled) {
        if (Settings.enabled && canStart(context)) {
            TrackerService.start(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "ActivityStack",
                largeTitle = "ActivityStack",
                subtitle = "实时捕捉前台 Activity",
                scrollBehavior = scrollBehavior,
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = paddingValues,
            ) {
                item { SmallTitle(text = "读取方式（二选一）") }
                item {
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        ShizukuRow(
                            binderAlive = binderAlive,
                            granted = shizukuGranted,
                            onAuthorize = { ShizukuHelper.requestPermission() },
                        )
                        RowDivider()
                        AccessibilityRow(
                            enabled = a11yEnabled,
                            onClick = { openAccessibilitySettings(context) },
                        )
                    }
                }

                item { SmallTitle(text = "运行环境") }
                item {
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        PermissionRow(
                            title = "悬浮窗权限",
                            summary = "用于在其他应用上层显示悬浮窗",
                            granted = overlayGranted,
                            onClick = { openOverlaySettings(context) },
                        )
                        RowDivider()
                        PermissionRow(
                            title = "通知权限",
                            summary = "用于后台常驻时的前台服务通知",
                            granted = notifyGranted,
                            onClick = { notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        )
                    }
                }

                item { SmallTitle(text = "实时前台") }
                item {
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        InfoContent(info = info, status = status)
                    }
                }

                item { SmallTitle(text = "悬浮窗") }
                item {
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        BasicComponent(
                            title = "启用悬浮窗",
                            summary = "开启后常驻显示当前前台 Activity",
                            onClick = {
                                val next = !enabled
                                if (next && !canStart(context)) {
                                    Toast.makeText(context, "请先完成 Shizuku 或无障碍授权，并授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                                    return@BasicComponent
                                }
                                enabled = next
                                Settings.enabled = next
                                if (next) TrackerService.start(context) else TrackerService.stop(context)
                            },
                            endActions = {
                                Switch(checked = enabled, onCheckedChange = null)
                            },
                        )
                        RowDivider()
                        BasicComponent(
                            title = "显示包名",
                            summary = "在悬浮窗中同时展示完整包名",
                            onClick = {
                                showPackage = !showPackage
                                Settings.showPackage = showPackage
                            },
                            endActions = {
                                Switch(checked = showPackage, onCheckedChange = null)
                            },
                        )
                    }
                }

                item { SmallTitle(text = "刷新间隔") }
                item {
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Settings.INTERVAL_OPTIONS.forEachIndexed { index, value ->
                            if (index > 0) RowDivider()
                            BasicComponent(
                                title = "$value ms",
                                onClick = {
                                    interval = value
                                    Settings.intervalMs = value
                                },
                                endActions = {
                                    RadioButton(selected = interval == value, onClick = null)
                                },
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(28.dp)) }
            }
        }
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun ShizukuRow(
    binderAlive: Boolean,
    granted: Boolean,
    onAuthorize: () -> Unit,
) {
    BasicComponent(
        title = "Shizuku",
        summary = when {
            !binderAlive -> "未检测到 Shizuku 服务，请先安装并启动"
            granted -> "已授权，可读取前台 Activity"
            else -> "服务已运行，点击授予授权"
        },
        enabled = binderAlive,
        onClick = { if (!granted) onAuthorize() },
        endActions = { StateTag(ok = granted) },
    )
}

@Composable
private fun AccessibilityRow(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = "无障碍服务（推荐）",
        summary = if (enabled) "已开启，可读取前台 Activity" else "与 Shizuku 二选一，普通用户推荐开启",
        onClick = onClick,
        endActions = { StateTag(ok = enabled) },
    )
}

@Composable
private fun PermissionRow(
    title: String,
    summary: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        summary = summary,
        onClick = { if (!granted) onClick() },
        endActions = { StateTag(ok = granted) },
    )
}

@Composable
private fun StateTag(ok: Boolean) {
    Text(
        text = if (ok) "已开启" else "去开启",
        style = MiuixTheme.textStyles.body2,
        color = if (ok) Palette.ok else Palette.warn,
    )
}

@Composable
private fun InfoContent(
    info: ForegroundActivity?,
    status: TrackerStatus,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info?.appLabel ?: "—",
                    style = MiuixTheme.textStyles.title3,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = info?.shortClassName ?: status.detail.ifEmpty { "尚未开始监听" },
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(
                text = "复制",
                enabled = info != null,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    info?.let {
                        context.getSystemService<ClipboardManager>()
                            ?.setPrimaryClip(ClipData.newPlainText("activity", it.flatten))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        InfoLine(label = "包名", value = info?.packageName ?: "—")
        InfoLine(label = "完整类名", value = info?.className ?: "—")
        InfoLine(label = "更新时间", value = info?.let { ActivityTracker.formatTime(it.updatedAt) } ?: "—")
        InfoLine(
            label = "状态",
            value = when (status.state) {
                TrackerState.RUNNING -> "运行中"
                TrackerState.NO_SHIZUKU -> "Shizuku 未运行"
                TrackerState.NO_PERMISSION -> "缺少 Shizuku 授权"
                TrackerState.ERROR -> "读取失败"
                TrackerState.IDLE -> "未启动"
            },
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun notificationGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun canStart(context: Context): Boolean =
    (ShizukuHelper.hasPermission() || AccessibilityTracker.isServiceEnabled(context))
        && Permissions.overlayGranted(context)

private fun openOverlaySettings(context: Context) {
    val intent = Intent(
        SystemSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(SystemSettings.ACTION_ACCESSIBILITY_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
