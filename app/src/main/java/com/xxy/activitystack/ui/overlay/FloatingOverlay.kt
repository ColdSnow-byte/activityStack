package com.xxy.activitystack.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xxy.activitystack.data.ActivityTracker
import com.xxy.activitystack.data.ForegroundActivity
import com.xxy.activitystack.data.Settings
import com.xxy.activitystack.data.TrackerState
import com.xxy.activitystack.data.TrackerStatus
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 悬浮窗内容，整体采用 Miuix 设计风格。
 *
 * @param scale        当前缩放比例（由应用内设置决定，范围见 [com.xxy.activitystack.service.FloatingWindow]）
 * @param onMeasure    面板原始（未缩放）尺寸回调，用于让 WindowManager 同步窗口大小
 */
@Composable
fun FloatingOverlay(
    scale: Float,
    onMeasure: (width: Int, height: Int) -> Unit,
    onDrag: (deltaX: Float, deltaY: Float) -> Unit,
    onCopy: (ForegroundActivity) -> Unit,
    onClose: () -> Unit,
) {
    val info by ActivityTracker.info.collectAsState()
    val status by ActivityTracker.status.collectAsState()
    val showPackage = Settings.showPackage

    Box(modifier = Modifier.fillMaxSize()) {
        // 内容层：以中心为锚点整体缩放，缩放后恰好填满窗口
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(scale, scale)
                .onGloballyPositioned { coords -> onMeasure(coords.size.width, coords.size.height) },
        ) {
            Panel(
                info = info,
                status = status,
                showPackage = showPackage,
                onDrag = onDrag,
                onCopy = onCopy,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun Panel(
    info: ForegroundActivity?,
    status: TrackerStatus,
    showPackage: Boolean,
    onDrag: (deltaX: Float, deltaY: Float) -> Unit,
    onCopy: (ForegroundActivity) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.72f))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.headline(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = info?.shortClassName ?: "获取中…",
                    style = MiuixTheme.textStyles.title3,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showPackage && info != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = info.packageName,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                PillButton(
                    text = "复制",
                    enabled = info != null,
                    primary = true,
                    onClick = { info?.let(onCopy) },
                )
                PillButton(
                    text = "关闭",
                    enabled = true,
                    primary = false,
                    onClick = onClose,
                )
            }
        }
    }
}

/** 半透明背景的按钮 */
@Composable
private fun PillButton(
    text: String,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.22f))
            .then(if (primary) Modifier else Modifier.widthIn(min = 48.dp)),
    ) {
        if (primary) {
            TextButton(
                text = text,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = onClick,
            )
        } else {
            TextButton(text = text, enabled = enabled, onClick = onClick)
        }
    }
}

@Composable
private fun TrackerStatus.headline(): String = when (state) {
    TrackerState.RUNNING -> "前台 Activity"
    TrackerState.NO_SHIZUKU -> "Shizuku 未运行"
    TrackerState.NO_PERMISSION -> "缺少 Shizuku 授权"
    TrackerState.ERROR -> "读取失败"
    TrackerState.IDLE -> "未启动"
}
