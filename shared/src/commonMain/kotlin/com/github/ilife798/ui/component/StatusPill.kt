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
import com.github.ilife798.data.model.DeviceDisplayStatus
import com.github.ilife798.ui.theme.RunningYellow
import com.github.ilife798.ui.theme.StatusOfflineBg
import com.github.ilife798.ui.theme.StatusOfflineText
import com.github.ilife798.ui.theme.StatusOnlineCyan
import com.github.ilife798.ui.theme.StatusStartingPink
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 状态胶囊徽标，首页与详情页共用
@Composable
fun StatusPill(status: DeviceDisplayStatus) {
    val background: Color
    val foreground: Color
    when (status) {
        DeviceDisplayStatus.Online -> {
            background = StatusOnlineCyan
            foreground = Color.White
        }

        DeviceDisplayStatus.Starting -> {
            background = StatusStartingPink
            foreground = Color.White
        }

        DeviceDisplayStatus.Running -> {
            background = RunningYellow
            foreground = Color(0xFF4A3A00)
        }

        DeviceDisplayStatus.Disabled,
        DeviceDisplayStatus.Offline,
        -> {
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
            text = status.label,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            color = foreground,
        )
    }
}
