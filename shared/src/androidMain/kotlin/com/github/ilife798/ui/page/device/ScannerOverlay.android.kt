package com.github.ilife798.ui.page.device

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ScannerOverlay(modifier: Modifier = Modifier) {
    val cornerColor = MiuixTheme.colorScheme.primary
    val scanProgress =
        rememberInfiniteTransition(label = "scanLine").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "scanLineProgress",
        )
    Canvas(modifier = modifier) {
        val side = size.minDimension * 0.68f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val scrim = Color.Black.copy(alpha = 0.55f)

        drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, top))
        drawRect(
            scrim,
            topLeft = Offset(0f, top + side),
            size = Size(size.width, size.height - (top + side)),
        )
        drawRect(scrim, topLeft = Offset(0f, top), size = Size(left, side))
        drawRect(
            scrim,
            topLeft = Offset(left + side, top),
            size = Size(size.width - (left + side), side),
        )

        val len = side * 0.16f
        val stroke = 4.dp.toPx()
        val right = left + side
        val bottom = top + side
        // 左上
        drawLine(cornerColor, Offset(left, top), Offset(left + len, top), stroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + len), stroke, StrokeCap.Round)
        // 右上
        drawLine(cornerColor, Offset(right - len, top), Offset(right, top), stroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + len), stroke, StrokeCap.Round)
        // 左下
        drawLine(cornerColor, Offset(left, bottom), Offset(left + len, bottom), stroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(left, bottom - len), Offset(left, bottom), stroke, StrokeCap.Round)
        // 右下
        drawLine(cornerColor, Offset(right - len, bottom), Offset(right, bottom), stroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(right, bottom - len), Offset(right, bottom), stroke, StrokeCap.Round)

        // 从上向下循环扫过的光带：两端延伸到框外，横向中间强两侧弱；光晕仅位于横线上方
        val scanY = top + side * scanProgress.value
        val extend = 26.dp.toPx()
        val lineLeft = left - extend
        val lineWidth = side + extend * 2f

        fun lineBrush(peakAlpha: Float): Brush =
            Brush.horizontalGradient(
                colors =
                    listOf(
                        Color.Transparent,
                        cornerColor.copy(alpha = peakAlpha * 0.25f),
                        cornerColor.copy(alpha = peakAlpha),
                        cornerColor.copy(alpha = peakAlpha * 0.25f),
                        Color.Transparent,
                    ),
                startX = lineLeft,
                endX = lineLeft + lineWidth,
            )

        // 横线上方的拖尾光晕：越往上越淡，衰减陡峭使亮部紧贴横线
        val steps = 12
        val trailHeight = 16.dp.toPx()
        for (i in 1..steps) {
            val frac = i / steps.toFloat()
            val h = trailHeight * frac
            val alpha = 0.8f * (1f - frac) * (1f - frac) + 0.02f
            drawRect(
                brush = lineBrush(alpha),
                topLeft = Offset(lineLeft, scanY - h),
                size = Size(lineWidth, h),
            )
        }

        // 横线本体
        val coreHeight = 2.dp.toPx()
        drawRect(
            brush = lineBrush(1f),
            topLeft = Offset(lineLeft, scanY - coreHeight / 2f),
            size = Size(lineWidth, coreHeight),
        )
    }
}
