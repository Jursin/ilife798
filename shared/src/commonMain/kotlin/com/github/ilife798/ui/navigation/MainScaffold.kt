package com.github.ilife798.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.ilife798.ui.theme.appBarBlur
import com.github.ilife798.ui.theme.blurAppBarColor
import com.github.ilife798.ui.theme.captureForBlur
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.AppShortcut
import com.github.ilife798.AppUpdateIntent
import com.github.ilife798.ui.page.home.HomePage
import com.github.ilife798.ui.page.tasks.TasksPage
import com.github.ilife798.ui.page.me.MePage
import com.github.ilife798.ui.page.login.LoginPage
import com.github.ilife798.ui.page.score.ScorePage
import com.github.ilife798.ui.page.account.AccountPage
import com.github.ilife798.ui.page.bill.MyBillPage
import com.github.ilife798.ui.page.device.DeviceAddPage
import com.github.ilife798.ui.page.device.QrScannerPage
import com.github.ilife798.ui.page.about.OpenSourceLicensePage

private val tabs = listOf(
    NavigationItem("首页", MiuixIcons.Home),
    NavigationItem("任务", MiuixIcons.ListView),
    NavigationItem("我的", MiuixIcons.Contacts)
)

@Composable
private fun NavBarContent(
    floatingNav: Boolean,
    navBarBackdrop: LayerBackdrop?,
    pagerState: PagerState,
    onSelect: (Int) -> Unit,
) {
    val blurActive = navBarBackdrop != null
    if (floatingNav) {
        FloatingNavigationBar(
            color = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
            modifier = if (blurActive) {
                Modifier.textureBlur(
                    backdrop = navBarBackdrop,
                    shape = RoundedCornerShape(50.dp),
                    blurRadius = 25f,
                    colors = BlurColors(
                        blendColors = listOf(
                            BlendColorEntry(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f))
                        )
                    ),
                )
            } else {
                Modifier
            },
        ) {
            tabs.forEachIndexed { index, tab ->
                FloatingNavigationBarItem(
                    selected = pagerState.currentPage == index,
                    onClick = { onSelect(index) },
                    icon = tab.icon,
                    label = tab.label
                )
            }
        }
    } else {
        NavigationBar(
            color = blurAppBarColor(navBarBackdrop),
            modifier = Modifier.appBarBlur(navBarBackdrop),
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = pagerState.currentPage == index,
                    onClick = { onSelect(index) },
                    icon = tab.icon,
                    label = tab.label
                )
            }
        }
    }
}

@Composable
private fun NavEntry(
    interceptPredictiveBack: Boolean,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = state,
        isBackEnabled = interceptPredictiveBack,
        onBackCompleted = onBack,
    )
    content()
}

@Composable
fun MainScaffold(viewModel: AppViewModel) {
    val backStack = rememberNavBackStack<Page>(Page.Home)
    val floatingNav = viewModel.state.floatingNav
    val predictiveBackEnabled = viewModel.state.predictiveBackEnabled

    val onBack: () -> Unit = remember(backStack) {
        { backStack.removeLastOrNull() }
    }

    // 防止连点重复压入相同路由（Navigation 3 不允许重复 key）
    val navigate: (Page) -> Unit = remember(backStack) {
        { page -> if (backStack.lastOrNull() != page) backStack.add(page) }
    }

    val interceptPredictiveBack = !predictiveBackEnabled && backStack.size > 1

    // 桌面快捷方式“扫一扫”：冷启动与 onNewIntent 均通过该信号跳转扫码页
    val scanRequestId = AppShortcut.scanRequestId
    var scanFromShortcut by remember { mutableStateOf(false) }
    LaunchedEffect(scanRequestId) {
        if (scanRequestId > 0) {
            scanFromShortcut = true
            navigate(Page.DeviceScan)
            AppShortcut.consumeScan()
        }
    }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val mainPagerState = rememberMainPagerState(pagerState, coroutineScope)
    val navBarBackdrop = rememberAppBlurBackdrop(viewModel.state.appBlur)

    // 快捷设置图块：回到首页并触发对应设备的“启动按钮”流程，停在弹出对话框
    val startDeviceId = AppShortcut.startDeviceId
    LaunchedEffect(startDeviceId) {
        val id = startDeviceId ?: return@LaunchedEffect
        while (backStack.size > 1) backStack.removeLastOrNull()
        mainPagerState.animateToPage(0)
        viewModel.requestExternalDeviceStart(id)
        AppShortcut.consumeStartDevice()
    }

    // 点击“正在下载”通知：回到“我的”页并重新弹出下载对话框
    val updateRequestId = AppUpdateIntent.showRequestId
    LaunchedEffect(updateRequestId) {
        if (updateRequestId <= 0) return@LaunchedEffect
        while (backStack.size > 1) backStack.removeLastOrNull()
        mainPagerState.animateToPage(2)
        viewModel.reopenUpdateProgressDialog()
        AppUpdateIntent.consume()
    }

    LaunchedEffect(pagerState.currentPage) {
        mainPagerState.syncPage()
        when (pagerState.currentPage) {
            0 -> {
                viewModel.loadDeviceInfo()
                viewModel.loadScoreInfo()
                viewModel.loadSpendingStats()
            }
            1 -> viewModel.loadMissions()
            2 -> viewModel.loadAccountInfo()
        }
    }

    // Tab-level back handler: go back to first tab when on non-first tab
    val isPagerBackHandlerEnabled by remember {
        androidx.compose.runtime.derivedStateOf {
            backStack.size == 1 && mainPagerState.selectedPage != 0
        }
    }
    val pagerBackEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = pagerBackEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = { mainPagerState.animateToPage(0) },
    )

    Box(modifier = Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)) {
        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            transition = NavTransitions.MiuixDefault,
            effects = NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = 32.dp,
                dimAmount = 0.5f,
                backdropColor = MiuixTheme.colorScheme.surface,
                blockInputDuringTransition = false,
            ),
        ) {
            entry<Page.Home> {
                NavEntry(interceptPredictiveBack, onBack) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = true,
                            beyondViewportPageCount = 1,
                            modifier = Modifier
                                .fillMaxSize()
                                .captureForBlur(navBarBackdrop),
                        ) { page ->
                            when (page) {
                                0 -> HomePage(
                                    viewModel = viewModel,
                                    onDeviceAddClick = { navigate(Page.DeviceAdd) }
                                )
                                1 -> TasksPage(
                                    viewModel = viewModel,
                                    onLoginClick = { isAlipay -> navigate(Page.Login(isAlipay = isAlipay)) }
                                )
                                2 -> MePage(
                                    viewModel = viewModel,
                                    onLoginClick = { isAlipay -> navigate(Page.Login(isAlipay = isAlipay)) },
                                    onScoreClick = { navigate(Page.Score) },
                                    onAccountClick = { navigate(Page.Account) },
                                    onBillClick = { navigate(Page.Bill) },
                                    onLicenseClick = { navigate(Page.Licenses) }
                                )
                            }
                        }
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            NavBarContent(
                                floatingNav = floatingNav,
                                navBarBackdrop = navBarBackdrop,
                                pagerState = pagerState,
                                onSelect = { mainPagerState.animateToPage(it) },
                            )
                        }
                    }
                }
            }
            entry<Page.Login> { key ->
                NavEntry(interceptPredictiveBack, onBack) {
                    LoginPage(
                        viewModel = viewModel,
                        isAlipay = key.isAlipay,
                        onBack = onBack
                    )
                }
            }
            entry<Page.Score> {
                NavEntry(interceptPredictiveBack, onBack) {
                    ScorePage(
                        viewModel = viewModel,
                        onBack = onBack
                    )
                }
            }
            entry<Page.Account> {
                NavEntry(interceptPredictiveBack, onBack) {
                    AccountPage(
                        viewModel = viewModel,
                        onBack = onBack
                    )
                }
            }
            entry<Page.Bill> {
                NavEntry(interceptPredictiveBack, onBack) {
                    MyBillPage(
                        viewModel = viewModel,
                        onBack = onBack
                    )
                }
            }
            entry<Page.DeviceAdd> {
                NavEntry(interceptPredictiveBack, onBack) {
                    DeviceAddPage(
                        viewModel = viewModel,
                        onBack = onBack,
                        onScanClick = {
                            scanFromShortcut = false
                            navigate(Page.DeviceScan)
                        }
                    )
                }
            }
            entry<Page.DeviceScan> {
                NavEntry(interceptPredictiveBack, onBack) {
                    QrScannerPage(
                        onBack = onBack,
                        onResult = { raw ->
                            viewModel.submitScannedRaw(raw)
                            onBack()
                            // 由快捷方式进入时，扫码后跳转添加设备页以便回填设备编号
                            if (scanFromShortcut) navigate(Page.DeviceAdd)
                        }
                    )
                }
            }
            entry<Page.Licenses> {
                NavEntry(interceptPredictiveBack, onBack) {
                    OpenSourceLicensePage(
                        viewModel = viewModel,
                        onBack = onBack
                    )
                }
            }
        }
    }
}
