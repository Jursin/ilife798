package com.github.ilife798.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private const val VIEWPORT = 16.97f

private fun svgIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()

val FlashlightOnIcon: ImageVector by lazy {
    svgIcon(
        name = "FlashlightOn",
        pathData = "M6.84,3.06c-.12-.12-.31-.12-.42,0s-.12.31,0,.42l.35.35c.12.12.31.12.42,0s.12-.31,0-.42l-.35-.35ZM8.78,2.12c0-.17-.14-.3-.31-.29-.16,0-.29.13-.29.29v1.27c0,.17.14.3.31.29.16,0,.29-.13.29-.29v-1.27ZM8.78,10.61c0-.17-.13-.3-.29-.31s-.3.13-.31.29c0,0,0,0,0,.01v.85c0,.17.13.3.29.31s.3-.13.31-.29c0,0,0,0,0-.01v-.85ZM5.09,7.64l1.61,1.61v3.64c0,.66.54,1.2,1.2,1.2,0,0,0,0,0,0h1.17c.66,0,1.2-.54,1.2-1.2v-3.64s1.61-1.61,1.61-1.61c.06-.06.09-.13.09-.21v-1.22c0-.66-.54-1.2-1.2-1.2h-4.56c-.66,0-1.2.54-1.2,1.2v1.22c0,.08.03.15.09.21M5.6,6.2c0-.33.27-.6.6-.6,0,0,0,0,0,0h4.56c.33,0,.6.27.6.6,0,0,0,0,0,0v.92h-5.76v-.92ZM6.03,7.72h4.92s-1.1,1.1-1.1,1.1h-2.72s-1.1-1.1-1.1-1.1ZM7.3,9.42h2.37s0,3.47,0,3.47c0,.33-.27.6-.6.6h-1.17c-.33,0-.6-.27-.6-.6,0,0,0,0,0,0v-3.47ZM9.78,3.41c-.12.12-.12.31,0,.42s.31.12.42,0l.35-.35c.12-.12.12-.31,0-.42s-.31-.12-.42,0l-.35.35Z"
    )
}

val FlashlightOffIcon: ImageVector by lazy {
    svgIcon(
        name = "FlashlightOff",
        pathData = "M2.12,8.18c-.17,0-.3.13-.3.3,0,.17.13.3.3.3h4.12s.46.46.46.46v3.64c0,.66.54,1.2,1.2,1.2,0,0,0,0,0,0h1.17c.66,0,1.2-.54,1.2-1.2v-3.64s.46-.46.46-.46h4.12c.17,0,.3-.13.3-.3s-.13-.3-.3-.3H2.12ZM11.37,6.69v.9s.55,0,.55,0c.03-.05.05-.1.05-.16v-1.22c0-.66-.54-1.2-1.2-1.2h-4.56c-.66,0-1.2.54-1.2,1.2v1.22c0,.06.02.11.05.16h.55s0-1.38,0-1.38c0-.33.27-.6.6-.6,0,0,0,0,0,0h4.56c.33,0,.6.27.6.6v.48s0,0,0,0ZM8.79,10.61c0-.17-.13-.3-.29-.31s-.3.13-.31.29c0,0,0,0,0,.01v.85c0,.17.13.3.29.31s.3-.13.31-.29c0,0,0,0,0-.01v-.85ZM7.3,9.42h2.37s0,3.47,0,3.47c0,.33-.27.6-.6.6h-1.17c-.33,0-.6-.27-.6-.6,0,0,0,0,0,0v-3.47Z"
    )
}
