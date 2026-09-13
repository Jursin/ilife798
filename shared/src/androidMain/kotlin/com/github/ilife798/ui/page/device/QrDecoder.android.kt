package com.github.ilife798.ui.page.device

import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.Binarizer
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

// 只解码画面中心区域（近似取景框），提升二维码相对占比与识别率
private const val QR_CROP_FRACTION = 0.8f

internal fun createQrReader(): MultiFormatReader =
    MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }

internal fun decodeQr(
    reader: MultiFormatReader,
    imageProxy: ImageProxy,
): String? {
    val plane = imageProxy.planes.firstOrNull() ?: return null
    val fullWidth = imageProxy.width
    val fullHeight = imageProxy.height
    if (fullWidth <= 0 || fullHeight <= 0) return null
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride

    // 中心正方形 ROI
    val minSide = minOf(fullWidth, fullHeight)
    val cropSize = (minSide * QR_CROP_FRACTION).toInt().coerceIn(1, minSide)
    val cropLeft = (fullWidth - cropSize) / 2
    val cropTop = (fullHeight - cropSize) / 2
    val data = ByteArray(cropSize * cropSize)
    val row = ByteArray(rowStride)
    for (y in 0 until cropSize) {
        val base = (cropTop + y) * rowStride
        if (base >= buffer.limit()) break
        buffer.position(base)
        val toRead = minOf(rowStride, buffer.remaining())
        buffer.get(row, 0, toRead)
        val dstOffset = y * cropSize
        if (pixelStride == 1) {
            System.arraycopy(row, cropLeft, data, dstOffset, cropSize)
        } else {
            var idx = cropLeft * pixelStride
            for (x in 0 until cropSize) {
                data[dstOffset + x] = row[idx]
                idx += pixelStride
            }
        }
    }

    val source = PlanarYUVLuminanceSource(data, cropSize, cropSize, 0, 0, cropSize, cropSize, false)
    // 先 HybridBinarizer，失败再试 GlobalHistogramBinarizer（应对反光/低对比）
    return tryDecode(reader, HybridBinarizer(source))
        ?: tryDecode(reader, GlobalHistogramBinarizer(source))
}

private fun tryDecode(
    reader: MultiFormatReader,
    binarizer: Binarizer,
): String? =
    try {
        reader.decodeWithState(BinaryBitmap(binarizer))?.text
    } catch (_: NotFoundException) {
        null
    } finally {
        reader.reset()
    }
