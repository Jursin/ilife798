package com.github.ilife798.ui.page.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Phone
import com.github.ilife798.ui.theme.captureForBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.ui.theme.appBarBlur
import com.github.ilife798.ui.theme.blurAppBarColor
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop

@Composable
fun LoginPage(viewModel: AppViewModel, isAlipay: Boolean = false, onBack: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var graphCode by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage
    val captchaImage = viewModel.captchaImage
    val smsSent = viewModel.smsSent
    val account = viewModel.state.account
    val dynamicColor = viewModel.state.dynamicColor

    // Local countdown - cancelled when leaving page
    var smsCountdown by remember { mutableIntStateOf(0) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            countdownJob?.cancel()
        }
    }

    val loginDone = if (isAlipay) account.pointsLoginDone else account.appToken.isNotEmpty()

    LaunchedEffect(loginDone) {
        if (loginDone) onBack()
    }

    // Load captcha on page enter
    LaunchedEffect(Unit) {
        viewModel.loadCaptcha()
    }

    val title = if (isAlipay) "积分登录" else "设备登录"
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(viewModel.state.appBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                modifier = Modifier.appBarBlur(blurBackdrop),
                color = blurAppBarColor(blurBackdrop),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearError()
                        viewModel.resetCaptcha()
                        onBack()
                    }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .captureForBlur(blurBackdrop)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollEndHaptic()
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Phone input
            TextField(
                value = phone,
                onValueChange = { if (it.length <= 11 && it.all { c -> c.isDigit() }) phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = "请输入手机号",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(
                        imageVector = MiuixIcons.Phone,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            )

            // Captcha input + captcha image
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = graphCode,
                    onValueChange = { if (it.length <= 5 && it.all { c -> c.isDigit() }) graphCode = it },
                    modifier = Modifier.weight(1f),
                    label = "请输入验证码",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = MiuixIcons.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                )
                if (captchaImage != null) {
                    Image(
                        bitmap = captchaImage,
                        contentDescription = "图形验证码",
                        modifier = Modifier
                            .size(width = 120.dp, height = 50.dp)
                            .clickable { viewModel.loadCaptcha() }
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .size(width = 120.dp, height = 50.dp)
                            .clickable { viewModel.loadCaptcha() }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "获取验证码",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }

            // SMS code input + send button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = smsCode,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) smsCode = it },
                    modifier = Modifier.weight(1f),
                    label = "请输入短信验证码",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = MiuixIcons.Community,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                )
                Button(
                    modifier = Modifier.width(120.dp),
                    onClick = {
                        viewModel.sendSmsCode(phone, graphCode)
                        countdownJob?.cancel()
                        smsCountdown = 60
                        countdownJob = scope.launch {
                            while (smsCountdown > 0 && isActive) {
                                delay(1000.milliseconds)
                                smsCountdown--
                            }
                            smsCountdown = 0
                        }
                    },
                    enabled = phone.length == 11 && graphCode.isNotBlank() && smsCountdown <= 0,
                    colors = primaryButtonColors(dynamicColor)
                ) {
                    Text(
                        text = if (smsCountdown > 0) "${smsCountdown}s" else "发送验证码"
                    )
                }
            }

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error
                )
            }

            // Login button
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.login(phone, smsCode, isAlipay) },
                enabled = !isLoading && smsCode.isNotEmpty() && smsSent,
                colors = primaryButtonColors(dynamicColor)
            ) {
                Text(text = "立即登录")
            }
        }
    }
}
