package com.github.ilife798.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.ilife798.ui.theme.RunningYellow
import com.github.ilife798.ui.theme.StatusOfflineBg
import com.github.ilife798.ui.theme.StatusOfflineText
import com.github.ilife798.ui.theme.StatusOnlineCyan
import com.github.ilife798.ui.theme.StatusStartingPink
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 设备状态文字（离线/在线/禁用/启动中/运行中），首页与详情页共用
fun statusTextFor(
    deviceStatus: Int,
    geneStatus: Int,
    dtype: Int,
): String =
    when {
        deviceStatus == 0 -> "离线"
        geneStatus == 99 -> "在线"
        geneStatus == 98 -> "禁用"
        (geneStatus == 1 || geneStatus == 30) && dtype in setOf(10, 80, 90, 120) -> "启动中"
        else -> "运行中"
    }

// 状态胶囊徽标，首页与详情页共用
@Composable
fun StatusPill(text: String) {
    val background: Color
    val foreground: Color
    when (text) {
        "在线" -> {
            background = StatusOnlineCyan
            foreground = Color.White
        }

        "启动中" -> {
            background = StatusStartingPink
            foreground = Color.White
        }

        "运行中" -> {
            background = RunningYellow
            foreground = Color(0xFF4A3A00)
        }

        else -> {
            background = StatusOfflineBg
            foreground = StatusOfflineText
        }
    }
    Box(
        modifier =
            Modifier
                .background(background, RoundedCornerShape(50.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            color = foreground,
        )
    }
}
