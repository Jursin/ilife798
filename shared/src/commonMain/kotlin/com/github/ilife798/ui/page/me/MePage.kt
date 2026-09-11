package com.github.ilife798.ui.page.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import com.github.ilife798.ui.theme.primaryButtonColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import com.github.ilife798.ui.theme.captureForBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Remove
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog
import com.github.ilife798.data.model.ThemeMode
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.getAppVersion
import com.github.ilife798.getAppVersionCode
import com.github.ilife798.ui.theme.WindowBlurEffect
import com.github.ilife798.ui.theme.appBarBlur
import com.github.ilife798.ui.theme.blurAppBarColor
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import top.yukonga.miuix.kmp.icon.extended.Create
import top.yukonga.miuix.kmp.icon.extended.Notes

@Composable
fun MePage(
    viewModel: AppViewModel,
    onLoginClick: (isAlipay: Boolean) -> Unit = {},
    onScoreClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onBillClick: () -> Unit = {},
    onLicenseClick: () -> Unit = {}
) {
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(viewModel.state.appBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                title = "我的",
                modifier = Modifier.appBarBlur(blurBackdrop),
                color = blurAppBarColor(blurBackdrop),
                scrollBehavior = scrollBehavior
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
                .padding(top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountSection(viewModel, onLoginClick, onScoreClick, onAccountClick, onBillClick)
            SettingsSection(viewModel, onLicenseClick)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountSection(
    viewModel: AppViewModel,
    onLoginClick: (isAlipay: Boolean) -> Unit,
    onScoreClick: () -> Unit,
    onAccountClick: () -> Unit,
    onBillClick: () -> Unit
) {
    val account = viewModel.state.account
    val accountInfo = viewModel.state.accountInfo
    val hasApp = account.appToken.isNotEmpty()
    val dynamicColor = viewModel.state.dynamicColor
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hasApp) {
        if (hasApp) viewModel.loadAccountInfo()
    }

    SmallTitle(text = "账号", insideMargin = PaddingValues(12.dp, 8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAccountClick() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (accountInfo.img.isNotEmpty()) {
                AsyncImage(
                    model = accountInfo.img,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MiuixIcons.Contacts,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountInfo.name.ifEmpty { if (account.isLoggedIn) account.phone else "未登录" },
                    style = MiuixTheme.textStyles.title2,
                    color = MiuixTheme.colorScheme.onSurface
                )
                if (accountInfo.pn.isNotEmpty()) {
                    Text(
                        text = accountInfo.pn,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
            if (hasApp) {
                IconButton(
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    onClick = { showLogoutDialog = true }
                ) {
                    Icon(
                        imageVector = MiuixIcons.Remove,
                        contentDescription = "退出登录",
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f)
                    )
                }
            } else {
                IconButton(
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    onClick = { onLoginClick(false) }
                ) {
                    Icon(
                        imageVector = MiuixIcons.Import,
                        contentDescription = "登录",
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f)
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onBillClick,
                    colors = primaryButtonColors(dynamicColor)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Notes,
                        contentDescription = "账单"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "我的账单")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onScoreClick,
                    colors = primaryButtonColors(dynamicColor)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Create,
                        contentDescription = "积分"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "积分明细")
                }
            }
        }
    }

    if (showLogoutDialog) {
        WindowDialog(
            show = true,
            onDismissRequest = { showLogoutDialog = false },
            title = "退出登录",
            content = {
                WindowBlurEffect(useBlur = viewModel.state.appBlur)
                val dismiss = LocalDismissState.current
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "确定要退出登录吗？")
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { dismiss?.invoke() },
                            text = "取消"
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.logout()
                                dismiss?.invoke()
                            },
                            text = "退出",
                            colors = ButtonDefaults.textButtonColors(
                                textColor = MiuixTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(viewModel: AppViewModel, onLicenseClick: () -> Unit) {
    val state = viewModel.state
    val uriHandler = LocalUriHandler.current
    val themeOptions = listOf("跟随系统", "浅色", "深色")
    var themeSelectedIndex by remember {
        mutableIntStateOf(
            when (state.themeMode) {
                ThemeMode.System -> 0
                ThemeMode.Light -> 1
                ThemeMode.Dark -> 2
            }
        )
    }
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    SmallTitle(text = "设置", insideMargin = PaddingValues(12.dp, 8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            OverlayDropdownPreference(
                title = "主题模式",
                items = themeOptions,
                selectedIndex = themeSelectedIndex,
                onSelectedIndexChange = { index ->
                    themeSelectedIndex = index
                    val mode = when (index) {
                        0 -> ThemeMode.System
                        1 -> ThemeMode.Light
                        else -> ThemeMode.Dark
                    }
                    viewModel.setThemeMode(mode)
                }
            )
            if (isRuntimeShaderSupported()) {
                BasicComponent(
                    title = "模糊效果",
                    summary = "为顶栏、底栏、对话框添加模糊效果",
                    endActions = {
                        Switch(
                            checked = state.appBlur,
                            onCheckedChange = { viewModel.setAppBlur(it) }
                        )
                    }
                )
            }
            BasicComponent(
                title = "动态取色",
                summary = "基于系统壁纸颜色生成配色方案",
                endActions = {
                    Switch(
                        checked = state.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            )
            BasicComponent(
                title = "预测性返回动画",
                summary = "返回滑动前提前预览即将跳转至的界面",
                endActions = {
                    Switch(
                        checked = state.predictiveBackEnabled,
                        onCheckedChange = { viewModel.setPredictiveBackEnabled(it) }
                    )
                }
            )
            BasicComponent(
                title = "悬浮底栏",
                summary = "切换悬浮式底部导航栏",
                endActions = {
                    Switch(
                        checked = state.floatingNav,
                        onCheckedChange = { viewModel.setFloatingNav(it) }
                    )
                }
            )
        }
    }
    SmallTitle(text = "关于", insideMargin = PaddingValues(12.dp, 8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            BasicComponent(
                title = "版本",
                summary = "${getAppVersion()}（${getAppVersionCode()}）"
            )
            BasicComponent(
                title = "查看源代码",
                summary = "在 GitHub 上查看源代码",
                onClick = { uriHandler.openUri("https://github.com/Jursin/ilife798") }
            )
            BasicComponent(
                title = "开放源代码许可",
                summary = "查看应用所使用的第三方开源库及其许可证信息",
                onClick = onLicenseClick
            )
            BasicComponent(
                title = "免责声明",
                summary = "查看使用本应用的免责声明",
                onClick = { showDisclaimerDialog = true }
            )
            BasicComponent(
                title = "赞助支持",
                summary = "在爱发电赞助我",
                onClick = { uriHandler.openUri("https://afdian.com/a/jursin") }
            )
        }
    }

    if (showDisclaimerDialog) {
        WindowDialog(
            show = true,
            onDismissRequest = { showDisclaimerDialog = false },
            title = "免责声明",
            content = {
                WindowBlurEffect(useBlur = state.appBlur)
                val dismiss = LocalDismissState.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        colors = CardDefaults.defaultColors(
                            color = MiuixTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            disclaimerLines.forEach { line ->
                                Text(
                                    text = "• $line",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { dismiss?.invoke() },
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(text = "了解")
                    }
                }
            }
        )
    }
}

private val disclaimerLines = listOf(
    "本项目非官方项目，与慧生活798服务提供方无任何联系。",
    "本项目仅供学习和研究使用，不用于商业目的。",
    "请在本应用上仅使用本人有权访问的账户和设备，并遵守相关服务协议。",
    "你应了解自动运行积分任务存在被官方标记、拉黑甚至追究的风险，因使用本应用造成严重后果的由使用者自行承担，本项目概不负责。",
    "你应了解官方可能会变更接口或相关认证方式，本项目不保证持续可用性，不一定及时通知或修复。"
)
