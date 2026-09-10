package com.github.ilife798.ui.page.device

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.github.ilife798.ui.icon.FlashlightOffIcon
import com.github.ilife798.ui.icon.FlashlightOnIcon
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private fun decodeQr(reader: MultiFormatReader, imageProxy: ImageProxy): String? {
    val plane = imageProxy.planes.firstOrNull() ?: return null
    val width = imageProxy.width
    val height = imageProxy.height
    if (width <= 0 || height <= 0) return null
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val data = ByteArray(width * height)
    if (pixelStride == 1 && rowStride == width) {
        buffer.get(data, 0, minOf(data.size, buffer.remaining()))
    } else {
        val row = ByteArray(rowStride)
        var offset = 0
        for (y in 0 until height) {
            val base = y * rowStride
            if (base >= buffer.limit()) break
            buffer.position(base)
            val toRead = minOf(rowStride, buffer.remaining())
            buffer.get(row, 0, toRead)
            for (x in 0 until width) {
                data[offset++] = row[x * pixelStride]
            }
        }
    }
    val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    return try {
        reader.decodeWithState(bitmap)?.text
    } catch (_: NotFoundException) {
        null
    } finally {
        reader.reset()
    }
}

@Composable
actual fun QrScannerPage(onBack: () -> Unit, onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalActivity.current as? LifecycleOwner

    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) showPermissionDialog = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 进入扫码页时强制收起可能残留的软键盘
    val rootView = LocalView.current
    LaunchedEffect(Unit) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(rootView.windowToken, 0)
    }

    val previewView = remember { PreviewView(context) }
    val handled = remember { AtomicBoolean(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    // 摄像头就绪或开关状态变化时应用手电筒
    LaunchedEffect(torchOn, camera) {
        camera?.cameraControl?.enableTorch(torchOn)
    }
    val reader = remember {
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true
                )
            )
        }
    }

    DisposableEffect(hasPermission, lifecycleOwner, previewView) {
        if (!hasPermission || lifecycleOwner == null) {
            return@DisposableEffect onDispose { }
        }
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { imageProxy ->
                try {
                    val value = decodeQr(reader, imageProxy)
                    if (!value.isNullOrEmpty() && handled.compareAndSet(false, true)) {
                        mainExecutor.execute { onResult(value) }
                    }
                } catch (_: Exception) {
                } finally {
                    imageProxy.close()
                }
            }
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (_: Exception) {
            }
        }, mainExecutor)

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            camera = null
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission && lifecycleOwner != null) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            ScannerOverlay(modifier = Modifier.fillMaxSize())
        }

        TopAppBar(
            title = "扫描二维码",
            modifier = Modifier.align(Alignment.TopCenter),
            color = Color.Transparent,
            titleColor = Color.White,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = MiuixIcons.Back, contentDescription = "返回", tint = Color.White)
                }
            }
        )

        IconButton(
            onClick = { torchOn = !torchOn },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-160).dp)
        ) {
            Icon(
                imageVector = if (torchOn) FlashlightOnIcon else FlashlightOffIcon,
                contentDescription = if (torchOn) "关闭手电筒" else "打开手电筒",
                modifier = Modifier.size(44.dp),
                tint = if (torchOn) MiuixTheme.colorScheme.primary else Color.White
            )
        }

        Text(
            text = "将设备二维码放入框内即可自动识别",
            color = Color.White.copy(alpha = 0.9f),
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )
    }

    if (showPermissionDialog) {
        WindowDialog(
            show = true,
            onDismissRequest = { showPermissionDialog = false },
            title = "需要相机权限",
            content = {
                val dismiss = LocalDismissState.current
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "扫描设备二维码需要相机权限，请在系统设置中开启。")
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            text = "取消",
                            modifier = Modifier.weight(1f),
                            onClick = { dismiss?.invoke() }
                        )
                        TextButton(
                            text = "去设置",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                dismiss?.invoke()
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    val cornerColor = MiuixTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val side = size.minDimension * 0.68f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val scrim = Color.Black.copy(alpha = 0.55f)

        drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, top))
        drawRect(
            scrim,
            topLeft = Offset(0f, top + side),
            size = Size(size.width, size.height - (top + side))
        )
        drawRect(scrim, topLeft = Offset(0f, top), size = Size(left, side))
        drawRect(
            scrim,
            topLeft = Offset(left + side, top),
            size = Size(size.width - (left + side), side)
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
    }
}
