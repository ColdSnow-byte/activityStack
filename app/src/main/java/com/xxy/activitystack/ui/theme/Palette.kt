package com.xxy.activitystack.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

object Palette {

    val ok: Color
        @Composable get() = MiuixTheme.colorScheme.primary

    val warn: Color
        @Composable get() = MiuixTheme.colorScheme.error

    val secondary: Color
        @Composable get() = MiuixTheme.colorScheme.onSurfaceVariantSummary
}
