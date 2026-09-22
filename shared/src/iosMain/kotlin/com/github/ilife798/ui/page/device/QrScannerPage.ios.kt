@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.ilife798.ui.page.device

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.github.ilife798.logDebug
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.concurrent.AtomicInt

// iOS 端扫码页：AVCaptureSession（.high）+ back 广角相机 + MetadataOutput(.qr)，
// 预览经 AVCaptureVideoPreviewLayer 嵌入 Compose UIKitView。
// 权限：进入页面先查状态，未决定即请求；被拒给明确提示与「前往设置」；拒绝不启动 session。
// 防重复回调：MetadataOutput 后台队列回调，首帧命中即 stopRunning 并回主线程 onResult 一次。
// 生命周期：会话操作串行化到专用队列，页面退出（或权限状态切换）时 stopRunning，防相机泄漏。
@Composable
actual fun QrScannerPage(
    onBack: () -> Unit,
    onResult: (String) -> Unit,
) {
    val currentOnResult by rememberUpdatedState(onResult)
    var authStatus by remember {
        mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(mediaType = AVMediaTypeVideo))
    }
    var cameraAvailable by remember { mutableStateOf(true) }

    val session = remember { AVCaptureSession() }
    val handled = remember { AtomicInt(0) }
    // 相机会话专用串行队列：start/stop 串行化，避免退出与启动竞态导致相机保持占用
    val cameraQueue = remember { dispatch_queue_create("ilife798.qrscanner.session", null) }
    val metadataDelegate =
        remember(handled) {
            QrMetadataDelegate(
                handled = handled,
                onResult = { value -> currentOnResult(value) },
            )
        }

    // 权限未决定：进入页面即请求相机权限（首次实际需要时请求）。
    LaunchedEffect(Unit) {
        if (authStatus == AVAuthorizationStatusNotDetermined) {
            AVCaptureDevice.requestAccessForMediaType(mediaType = AVMediaTypeVideo) { granted ->
                dispatch_async(dispatch_get_main_queue()) {
                    authStatus =
                        if (granted) AVAuthorizationStatusAuthorized else AVAuthorizationStatusDenied
                }
            }
        }
    }

    // 从系统设置返回（前往设置开启权限后）：回前台时重读权限状态，触发 effect 重启。
    DisposableEffect(Unit) {
        val observer =
            NSNotificationCenter.defaultCenter.addObserverForName(
                name = UIApplicationWillEnterForegroundNotification,
                `object` = null,
                queue = null,
            ) { _ ->
                authStatus =
                    AVCaptureDevice.authorizationStatusForMediaType(mediaType = AVMediaTypeVideo)
            }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    // 已授权：串行队列配置并启动相机；权限状态变化或页面退出时停止会话。
    DisposableEffect(authStatus) {
        if (authStatus == AVAuthorizationStatusAuthorized) {
            // 首次授权流程：旧 effect 的 onDispose 已把 handled 置 1，重启相机前复位
            handled.value = 0
            dispatch_async(cameraQueue) {
                startCameraSession(session, metadataDelegate, cameraQueue) { available ->
                    dispatch_async(dispatch_get_main_queue()) { cameraAvailable = available }
                }
            }
        }
        onDispose {
            handled.value = 1
            dispatch_async(cameraQueue) {
                runCatching { session.stopRunning() }
            }
        }
    }

    val denied =
        authStatus == AVAuthorizationStatusDenied || authStatus == AVAuthorizationStatusRestricted

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (authStatus == AVAuthorizationStatusAuthorized && cameraAvailable) {
            UIKitView(
                factory = { CameraPreviewView(session) },
                update = { preview -> preview.syncFrame() },
                onRelease = { preview -> preview.detach() },
                modifier = Modifier.fillMaxSize(),
            )
            // 中央取景框与提示文案
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(240.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "将设备二维码放入框内即可自动识别",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MiuixTheme.textStyles.body2,
                )
            }
        } else {
            // 相机不可用：权限被拒/受限，或设备无摄像头（模拟器路径）——明确失败反馈，不崩溃
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (denied) "扫码需要相机权限，当前未授权" else "当前设备没有可用相机，无法扫码",
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    style = MiuixTheme.textStyles.body2,
                )
                if (denied) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(MiuixTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                                .clickable {
                                    // 前往系统设置开启相机权限
                                    val settingsUrl = NSURL(string = UIApplicationOpenSettingsURLString)
                                    runCatching { UIApplication.sharedApplication.openURL(settingsUrl) }
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "前往设置", color = Color.White, style = MiuixTheme.textStyles.body2)
                    }
                }
            }
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
    }
}

// 二维码识别回调：MetadataOutput 在后台队列派发；首帧命中即停会话并回主线程上报一次。
private class QrMetadataDelegate(
    private val handled: AtomicInt,
    private val onResult: (String) -> Unit,
) : NSObject(),
    AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        if (handled.value == 1) return
        val value =
            didOutputMetadataObjects
                .filterIsInstance<AVMetadataMachineReadableCodeObject>()
                .firstOrNull { it.stringValue != null }
                ?.stringValue
                ?: return
        if (!handled.compareAndSet(0, 1)) return
        dispatch_async(dispatch_get_main_queue()) {
            onResult(value)
        }
    }
}

// 预览容器：AVCaptureVideoPreviewLayer 作为子层；布局变化与 update 阶段同步 frame。
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private class CameraPreviewView(
    session: AVCaptureSession,
) : UIView(frame = CGRectZero.readValue()) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)

    init {
        layer.addSublayer(previewLayer)
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        syncFrame()
    }

    fun syncFrame() {
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        previewLayer.setFrame(bounds)
        CATransaction.commit()
    }

    fun detach() {
        previewLayer.removeFromSuperlayer()
    }
}

// 会话配置与启动：必须在串行队列执行（startRunning 阻塞）。
// 设备缺失（模拟器）/输入输出挂载失败/异常 → 回调 false，页面降级提示；不向调用方抛异常。
private fun startCameraSession(
    session: AVCaptureSession,
    metadataDelegate: QrMetadataDelegate,
    callbackQueue: dispatch_queue_t,
    onCameraAvailable: (Boolean) -> Unit,
) {
    try {
        if (session.inputs.isNotEmpty()) {
            // 已配置过（权限状态切换重入）：直接恢复运行
            session.startRunning()
            onCameraAvailable(true)
            return
        }
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPresetHigh
        val device =
            AVCaptureDevice.defaultDeviceWithDeviceType(
                deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera,
                mediaType = AVMediaTypeVideo,
                position = AVCaptureDevicePositionBack,
            )
        val input =
            device?.let { AVCaptureDeviceInput(device = it, error = null) }
        if (input == null || !session.canAddInput(input)) {
            session.commitConfiguration()
            onCameraAvailable(false)
            return
        }
        session.addInput(input)
        val output = AVCaptureMetadataOutput()
        if (!session.canAddOutput(output)) {
            session.commitConfiguration()
            onCameraAvailable(false)
            return
        }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(
            metadataDelegate,
            queue = callbackQueue,
        )
        output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
        session.commitConfiguration()
        session.startRunning()
        onCameraAvailable(true)
    } catch (e: Exception) {
        logDebug("QrScannerPage", "camera start failed: ${e.message}")
        onCameraAvailable(false)
    }
}
