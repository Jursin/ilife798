package com.github.ilife798

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

// 图片解码：Skia；解码失败返回占位空 bitmap，不抛异常。
actual fun ByteArray.toImageBitmap(): ImageBitmap =
    runCatching {
        Image.makeFromEncoded(this).toComposeImageBitmap()
    }.getOrElse {
        ImageBitmap(1, 1)
    }
