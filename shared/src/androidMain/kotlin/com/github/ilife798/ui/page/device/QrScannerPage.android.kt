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
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.github.ilife798.shared.resources.Res
import com.github.ilife798.shared.resources.ic_torch_off
import com.github.ilife798.shared.resources.ic_torch_on
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
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
import kotlin.time.Duration.Companion.milliseconds
import android.util.Size as AndroidSize

@Composable
actual fun QrScannerPage(
    onBack: () -> Unit,
    onResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalActivity.current as? LifecycleOwner

    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
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

    // 摄像头就绪后先对中心做一次自动对焦
    LaunchedEffect(camera) {
        val cam = camera ?: return@LaunchedEffect
        delay(500.milliseconds)
        val cx = previewView.width / 2f
        val cy = previewView.height / 2f
        if (cx > 0f && cy > 0f) {
            runCatching {
                cam.cameraControl.startFocusAndMetering(
                    FocusMeteringAction.Builder(previewView.meteringPointFactory.createPoint(cx, cy)).build(),
                )
            }
        }
    }
    val reader = remember { createQrReader() }

    DisposableEffect(hasPermission, lifecycleOwner, previewView) {
        if (!hasPermission || lifecycleOwner == null) {
            return@DisposableEffect onDispose { }
        }
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()
            val preview =
                Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
            val resolutionSelector =
                ResolutionSelector
                    .Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            AndroidSize(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    ).build()
            val analysis =
                ImageAnalysis
                    .Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(resolutionSelector)
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
                camera =
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
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
            // 点击预览区域对焦
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(camera) {
                            detectTapGestures { offset ->
                                val cam = camera ?: return@detectTapGestures
                                runCatching {
                                    cam.cameraControl.startFocusAndMetering(
                                        FocusMeteringAction
                                            .Builder(
                                                previewView.meteringPointFactory.createPoint(offset.x, offset.y),
                                            ).build(),
                                    )
                                }
                            }
                        },
            )
            ScannerOverlay(modifier = Modifier.fillMaxSize())
        }

        TopAppBar(
            title = "扫描二维码",
            modifier = Modifier.align(Alignment.TopCenter),
            color = Color.Transparent,
            largeTitleColor = Color.White,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = MiuixIcons.Back, contentDescription = "返回", tint = Color.White)
                }
            },
        )

        Icon(
            painter = painterResource(if (torchOn) Res.drawable.ic_torch_on else Res.drawable.ic_torch_off),
            contentDescription = if (torchOn) "关闭手电筒" else "打开手电筒",
            tint = Color.Unspecified,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-160).dp)
                    .size(width = 30.dp, height = 56.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { torchOn = !torchOn },
                    ),
        )

        Text(
            text = "将设备二维码放入框内即可自动识别",
            color = Color.White.copy(alpha = 0.9f),
            style = MiuixTheme.textStyles.body2,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp),
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "扫描设备二维码需要相机权限，请在系统设置中开启。")
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            text = "取消",
                            modifier = Modifier.weight(1f),
                            onClick = { dismiss?.invoke() },
                        )
                        TextButton(
                            text = "去设置",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                dismiss?.invoke()
                                val intent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            },
        )
    }
}
