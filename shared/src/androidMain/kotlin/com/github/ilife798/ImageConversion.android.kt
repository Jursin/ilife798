package com.github.ilife798

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val bitmap =
        BitmapFactory.decodeByteArray(this, 0, this.size)
            ?: throw IllegalStateException("图片解码失败（响应 ${size}B，非图片内容）")
    return bitmap.asImageBitmap()
}
