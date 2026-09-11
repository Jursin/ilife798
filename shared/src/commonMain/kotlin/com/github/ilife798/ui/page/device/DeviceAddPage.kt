package com.github.ilife798.ui.page.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import com.github.ilife798.ui.theme.primaryButtonColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.github.ilife798.ui.theme.captureForBlur
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.ui.theme.appBarBlur
import com.github.ilife798.ui.theme.blurAppBarColor
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Scan

@Composable
fun DeviceAddPage(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onScanClick: () -> Unit = {}
) {
    var deviceId by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scrollBehavior = MiuixScrollBehavior()
    val dynamicColor = viewModel.state.dynamicColor
    val blurBackdrop = rememberAppBlurBackdrop(viewModel.state.appBlur)

    // 扫码解析完成后（在 ViewModel 中执行）回填设备编号，避免在 composition 协程里发起请求
    val scannedDeviceId = viewModel.scannedDeviceId
    LaunchedEffect(scannedDeviceId) {
        if (scannedDeviceId != null) {
            deviceId = scannedDeviceId
            viewModel.consumeScannedDeviceId()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "添加设备",
                modifier = Modifier.appBarBlur(blurBackdrop),
                color = blurAppBarColor(blurBackdrop),
                defaultWindowInsetsPadding = false,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .captureForBlur(blurBackdrop)
                .scrollEndHaptic()
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = deviceId,
                        onValueChange = { deviceId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "设备编号"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                focusManager.clearFocus()
                                onScanClick()
                            },
                            colors = primaryButtonColors(dynamicColor)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Scan,
                                contentDescription = "扫描"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "扫一扫")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (deviceId.isNotBlank()) {
                                    viewModel.addDevice(deviceId.trim())
                                    onBack()
                                }
                            },
                            enabled = deviceId.isNotBlank(),
                            colors = primaryButtonColors(dynamicColor)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = "添加"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "添加")
                        }
                    }
                }
            }
        }
    }
}
