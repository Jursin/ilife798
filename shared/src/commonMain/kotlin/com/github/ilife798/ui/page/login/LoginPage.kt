package com.github.ilife798.ui.page.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.ui.theme.appBarBlur
import com.github.ilife798.ui.theme.blurAppBarColor
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop

@Composable
fun LoginPage(viewModel: AppViewModel, isAlipay: Boolean = false, onBack: () -> Unit) {
    val account = viewModel.state.account
    val accountInfo = viewModel.state.accountInfo
    var phone by remember {
        val initial = account.phone.ifEmpty { accountInfo.pn }
        mutableStateOf(TextFieldValue(text = initial, selection = TextRange(initial.length)))
    }
    var graphCode by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage
    val captchaImage = viewModel.captchaImage
    val smsSent = viewModel.smsSent
    val dynamicColor = viewModel.state.dynamicColor
    val phoneFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

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

    // 验证码刷新后清空旧输入
    LaunchedEffect(viewModel.captchaKey) {
        if (viewModel.captchaKey.isNotEmpty()) graphCode = ""
    }

    // 已有一次登录（本页或另一渠道）时，自动回填手机号，仅在输入框为空时填充，光标置于末尾
    LaunchedEffect(account.phone, accountInfo.pn) {
        if (phone.text.isEmpty()) {
            val saved = account.phone.ifEmpty { accountInfo.pn }
            if (saved.isNotEmpty()) {
                phone = TextFieldValue(text = saved, selection = TextRange(saved.length))
            }
        }
    }

    // 进入页面自动聚焦手机号输入框并弹出键盘
    LaunchedEffect(Unit) {
        phoneFocusRequester.requestFocus()
        keyboardController?.show()
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
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                }
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
                onValueChange = { new ->
                    if (new.text.length <= 11 && new.text.all { c -> c.isDigit() }) phone = new
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(phoneFocusRequester),
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
                        viewModel.sendSmsCode(phone.text, graphCode)
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
                    enabled = phone.text.length == 11 && graphCode.isNotBlank() && smsCountdown <= 0,
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
                onClick = { viewModel.login(phone.text, smsCode, isAlipay) },
                enabled = !isLoading && smsCode.isNotEmpty() && smsSent,
                colors = primaryButtonColors(dynamicColor)
            ) {
                Text(text = "立即登录")
            }
        }
    }
}
