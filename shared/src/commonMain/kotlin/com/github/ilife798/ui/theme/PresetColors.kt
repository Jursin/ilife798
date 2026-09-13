package com.github.ilife798.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.ilife798.data.model.PaletteStyle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController

data class PresetColor(
    val label: String,
    val color: Color,
)

val PresetColors =
    listOf(
        PresetColor("默认", Color(0xFF4A672D)),
        PresetColor("粉色", Color(0xFFB94073)),
        PresetColor("红色", Color(0xFFBA1A1A)),
        PresetColor("橙色", Color(0xFF944A00)),
        PresetColor("琥珀", Color(0xFF8C5300)),
        PresetColor("黄色", Color(0xFF795900)),
        PresetColor("青柠", Color(0xFF5E6400)),
        PresetColor("绿色", Color(0xFF006D39)),
        PresetColor("青色", Color(0xFF006A64)),
        PresetColor("蓝绿", Color(0xFF006874)),
        PresetColor("浅蓝", Color(0xFF00639B)),
        PresetColor("蓝色", Color(0xFF335BBC)),
        PresetColor("靛蓝", Color(0xFF5355A9)),
        PresetColor("紫色", Color(0xFF6750A4)),
        PresetColor("深紫", Color(0xFF7E42A4)),
        PresetColor("蓝灰", Color(0xFF575D7E)),
        PresetColor("棕色", Color(0xFF7D524A)),
        PresetColor("灰色", Color(0xFF5F6162)),
    )

@Composable
fun ColorSwatchPreview(
    preset: PresetColor,
    paletteStyle: PaletteStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val controller =
        remember(preset.color, paletteStyle) {
            ThemeController(
                colorSchemeMode = ColorSchemeMode.MonetLight,
                keyColor = preset.color,
                paletteStyle = paletteStyle.toMiuixPaletteStyle(),
                colorSpec = ThemeColorSpec.Spec2025,
                isDark = false,
            )
        }
    val scheme = controller.currentColors()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .background(scheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = scheme.primaryContainer.copy(alpha = 0.9f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                    )
                    drawArc(
                        color = scheme.tertiaryContainer.copy(alpha = 0.9f),
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true,
                    )
                    drawArc(
                        color = scheme.secondaryContainer.copy(alpha = 0.6f),
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(scheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = MiuixIcons.Ok,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = preset.label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
