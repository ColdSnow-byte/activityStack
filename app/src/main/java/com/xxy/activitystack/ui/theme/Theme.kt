package com.xxy.activitystack.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 全应用统一的 Miuix（HyperOS 风格）主题。
 */
@Composable
fun ActivityStackTheme(
    mode: ColorSchemeMode = ColorSchemeMode.System,
    content: @Composable () -> Unit,
) {
    val controller = remember(mode) { ThemeController(mode) }
    MiuixTheme(controller = controller) {
        content()
    }
}
