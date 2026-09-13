package com.github.ilife798.ui.page.me

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.github.ilife798.data.model.PaletteStyle
import com.github.ilife798.data.model.ThemeMode
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.getAppVersion
import com.github.ilife798.getAppVersionCode
import com.github.ilife798.showToast
import com.github.ilife798.ui.component.SwitchPreference
import com.github.ilife798.ui.theme.ColorSwatchPreview
import com.github.ilife798.ui.theme.PresetColors
import com.github.ilife798.ui.theme.WindowBlurEffect
import com.github.ilife798.util.currentTimeMillis
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun SettingsSection(
    viewModel: AppViewModel,
    onLicenseClick: () -> Unit,
    wideScreen: Boolean,
) {
    val state = viewModel.state
    val uriHandler = LocalUriHandler.current
    val themeOptions = listOf("跟随系统", "浅色", "深色")
    var themeSelectedIndex by remember {
        mutableIntStateOf(
            when (state.themeMode) {
                ThemeMode.System -> 0
                ThemeMode.Light -> 1
                ThemeMode.Dark -> 2
            },
        )
    }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showGithubProxyDialog by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastVersionTapAt by remember { mutableStateOf(0L) }

    SmallTitle(text = "外观设置", insideMargin = PaddingValues(12.dp, 8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            OverlayDropdownPreference(
                title = "主题模式",
                items = themeOptions,
                selectedIndex = themeSelectedIndex,
                onSelectedIndexChange = { index ->
                    themeSelectedIndex = index
                    val mode =
                        when (index) {
                            0 -> ThemeMode.System
                            1 -> ThemeMode.Light
                            else -> ThemeMode.Dark
                        }
                    viewModel.setThemeMode(mode)
                },
            )
            if (isRuntimeShaderSupported()) {
                SwitchPreference(
                    title = "模糊效果",
                    summary = "为顶栏、底栏、对话框添加模糊效果",
                    checked = state.appBlur,
                    onCheckedChange = { viewModel.setAppBlur(it) },
                )
            }
            SwitchPreference(
                title = "预测性返回动画",
                summary = "返回滑动前提前预览即将跳转至的界面",
                checked = state.predictiveBackEnabled,
                onCheckedChange = { viewModel.setPredictiveBackEnabled(it) },
            )
            SwitchPreference(
                title = "自定义颜色",
                summary = "自定义应用主题配色方案",
                checked = state.customColor,
                onCheckedChange = { viewModel.setCustomColor(it) },
            )
            AnimatedVisibility(
                visible = state.customColor,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SwitchPreference(
                    title = "动态取色",
                    summary = "基于系统壁纸颜色生成配色方案",
                    checked = state.dynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) },
                )
            }
            AnimatedVisibility(
                visible = state.customColor,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                OverlayDropdownPreference(
                    title = "调色板风格",
                    items = PaletteStyle.entries.map { it.displayName },
                    selectedIndex = PaletteStyle.entries.indexOf(state.paletteStyle).coerceAtLeast(0),
                    onSelectedIndexChange = { index ->
                        viewModel.setPaletteStyle(PaletteStyle.entries[index])
                    },
                )
            }
            AnimatedVisibility(
                visible = state.customColor && !state.dynamicColor,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                PresetColorGrid(
                    paletteStyle = state.paletteStyle,
                    selectedSeed = state.seedColor,
                    onSelect = { viewModel.setSeedColor(it) },
                )
            }
            if (!wideScreen) {
                SwitchPreference(
                    title = "悬浮底栏",
                    summary = "切换悬浮式底部导航栏",
                    checked = state.floatingNav,
                    onCheckedChange = { viewModel.setFloatingNav(it) },
                )
            }
        }
    }
    SmallTitle(text = "更新设置", insideMargin = PaddingValues(12.dp, 8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            ArrowPreference(
                title = "检查更新",
                summary = "从 GitHub 检查最新版本",
                onClick = { viewModel.checkForUpdate() },
            )
            SwitchPreference(
                title = "启动时检查更新",
                summary = "打开应用时自动检查新版本",
                checked = viewModel.checkUpdateOnStart,
                onCheckedChange = { viewModel.setCheckUpdateOnStartEnabled(it) },
            )
            ArrowPreference(
                title = "加速地址",
                summary = viewModel.githubProxyUrl.ifBlank { "设置 GitHub 加速地址" },
                onClick = { showGithubProxyDialog = true },
            )
        }
    }
    SmallTitle(text = "关于", insideMargin = PaddingValues(12.dp, 8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            BasicComponent(
                title = "版本",
                summary = "${getAppVersion()}(${getAppVersionCode()})",
                modifier =
                    Modifier.combinedClickable(
                        onClick = {
                            val now = currentTimeMillis()
                            versionTapCount = if (now - lastVersionTapAt > 2000L) 1 else versionTapCount + 1
                            lastVersionTapAt = now
                            if (versionTapCount >= 5) {
                                versionTapCount = 0
                                if (viewModel.developerMode) {
                                    viewModel.disableDeveloperMode()
                                    showToast("已关闭开发者模式")
                                } else {
                                    viewModel.enableDeveloperMode()
                                    showToast("已启用开发者模式")
                                }
                            }
                        },
                    ),
            )
            ArrowPreference(
                title = "查看源代码",
                summary = "在 GitHub 上查看源代码",
                onClick = { uriHandler.openUri("https://github.com/Jursin/ilife798") },
            )
            ArrowPreference(
                title = "开放源代码许可",
                summary = "查看应用所使用的第三方开源库及其许可证信息",
                onClick = onLicenseClick,
            )
            ArrowPreference(
                title = "免责声明",
                summary = "查看使用本应用的免责声明",
                onClick = { showDisclaimerDialog = true },
            )
            ArrowPreference(
                title = "赞助支持",
                summary = "在爱发电赞助我",
                onClick = { uriHandler.openUri("https://afdian.com/a/jursin") },
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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                ) {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                        colors =
                            CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.surfaceContainerHighest,
                            ),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            disclaimerLines.forEach { line ->
                                Text(
                                    text = "• $line",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { dismiss?.invoke() },
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(text = "了解")
                    }
                }
            },
        )
    }

    UpdateDialogs(
        viewModel = viewModel,
        showGithubProxyDialog = showGithubProxyDialog,
        onDismissGithubProxyDialog = { showGithubProxyDialog = false },
    )
}

@Composable
private fun PresetColorGrid(
    paletteStyle: PaletteStyle,
    selectedSeed: Int,
    onSelect: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        val columns = (maxWidth / 80.dp).toInt().coerceAtLeast(1)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PresetColors.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    rowItems.forEach { preset ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            ColorSwatchPreview(
                                preset = preset,
                                paletteStyle = paletteStyle,
                                selected = selectedSeed == preset.color.toArgb(),
                                onClick = { onSelect(preset.color.toArgb()) },
                            )
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private val disclaimerLines =
    listOf(
        "本项目非官方项目，与慧生活798服务提供方无任何联系。",
        "本项目仅供学习和研究使用，不用于商业目的。",
        "请在本应用上仅使用本人有权访问的账户和设备，并遵守相关服务协议。",
        "你应了解自动运行积分任务存在被官方标记、拉黑甚至追究的风险，因使用本应用造成严重后果的由使用者自行承担，本项目概不负责。",
        "你应了解官方可能会变更接口或相关认证方式，本项目不保证持续可用性，不一定及时通知或修复。",
    )
