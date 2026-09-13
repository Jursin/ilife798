package com.github.ilife798.ui.page.score

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog
import com.github.ilife798.data.model.ScoreFilter
import com.github.ilife798.data.model.ScoreRecord
import com.github.ilife798.data.model.WalletAccount
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.showToast
import com.github.ilife798.ui.theme.WindowBlurEffect
import com.github.ilife798.ui.theme.appBarBlur
import com.github.ilife798.ui.theme.blurAppBarColor
import com.github.ilife798.ui.theme.captureForBlur
import com.github.ilife798.ui.theme.primaryButtonColors
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.github.ilife798.util.formatTimestamp
import kotlin.math.round

private const val LOAD_MORE_THRESHOLD_PX = 200

// 积分兑换档位（积分）
private val SCORE_EXCHANGE_AMOUNTS = listOf(100, 1000)

private enum class ScorePanel { Records, Exchange }

@Composable
fun ScorePage(viewModel: AppViewModel, onBack: () -> Unit) {
    val state = viewModel.state
    val scores = viewModel.scoreRecords
    val isLoggedIn = state.account.appToken.isNotEmpty() || state.account.token.isNotEmpty()
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var panel by remember { mutableStateOf(ScorePanel.Records) }
    var exchangeUnitScore by remember { mutableStateOf<Int?>(null) }

    val scoreFilterEntry = remember(viewModel.scoreFilter) {
        DropdownEntry(
            items = ScoreFilter.entries.map { filter ->
                DropdownItem(
                    text = scoreFilterLabel(filter),
                    selected = viewModel.scoreFilter == filter,
                    onClick = { viewModel.selectScoreFilter(filter) }
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        if (state.account.appToken.isNotEmpty() || state.account.token.isNotEmpty()) {
            viewModel.refreshScorePage()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val max = scrollState.maxValue
            max > 0 && scrollState.value >= max - LOAD_MORE_THRESHOLD_PX
        }
    }
    LaunchedEffect(shouldLoadMore, scores.size) {
        if (shouldLoadMore && panel == ScorePanel.Records) viewModel.loadMoreScores()
    }
    LaunchedEffect(viewModel.scoreFilter) {
        scrollState.animateScrollTo(0)
    }
    LaunchedEffect(panel) {
        scrollState.animateScrollTo(0)
        if (panel == ScorePanel.Exchange && state.wallets.isEmpty()) viewModel.loadWallet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "积分明细",
                modifier = Modifier.appBarBlur(blurBackdrop),
                color = blurAppBarColor(blurBackdrop),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    OverlayIconDropdownMenu(entry = scoreFilterEntry) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "选择积分类型"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = viewModel.scoreRefreshing,
            onRefresh = { viewModel.refreshScores() },
            modifier = Modifier.fillMaxSize(),
            topAppBarScrollBehavior = scrollBehavior,
            contentPadding = paddingValues,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            refreshTexts = listOf("下拉刷新", "松开刷新", "正在刷新…", "刷新完成")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .captureForBlur(blurBackdrop)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .verticalScroll(scrollState)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "积分概览",
                            style = MiuixTheme.textStyles.title2,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ScoreSummaryItem(
                                label = "可用积分",
                                value = state.points.available,
                                alignEnd = false
                            )
                            ScoreSummaryItem(
                                label = "累计积分",
                                value = state.points.total,
                                alignEnd = true
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                panel = if (panel == ScorePanel.Exchange) {
                                    ScorePanel.Records
                                } else {
                                    ScorePanel.Exchange
                                }
                            },
                            colors = if (panel == ScorePanel.Exchange) {
                                ButtonDefaults.buttonColorsPrimary()
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Text(text = "积分兑换")
                        }
                    }
                }

                AnimatedContent(
                    targetState = panel,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 10 }) togetherWith
                            (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 10 })
                    },
                    label = "scorePanel"
                ) { target ->
                    when (target) {
                        ScorePanel.Records -> ScoreRecordsCard(
                            records = scores,
                            loadingMore = viewModel.scoreLoadingMore,
                            hasMore = viewModel.scoreHasMore,
                            isLoggedIn = isLoggedIn
                        )

                        ScorePanel.Exchange -> ScoreExchangeCard(
                            wallets = state.wallets,
                            activeWalletId = state.activeWalletId,
                            available = state.points.available ?: 0,
                            submitting = viewModel.exchangeSubmitting,
                            dynamicColor = state.dynamicColor,
                            isLoggedIn = isLoggedIn,
                            onSelectWallet = { viewModel.selectWallet(it) },
                            onRequestExchange = { exchangeUnitScore = it }
                        )
                    }
                }
            }
        }
    }

    exchangeUnitScore?.let { unitScore ->
        val wallet = state.wallets.firstOrNull { it.id == state.activeWalletId }
            ?: state.wallets.firstOrNull()
        ScoreExchangeDialog(
            unitScore = unitScore,
            walletName = wallet?.name?.ifEmpty { "钱包" } ?: "钱包",
            available = state.points.available ?: 0,
            appBlur = state.appBlur,
            dynamicColor = state.dynamicColor,
            submitting = viewModel.exchangeSubmitting,
            onDismiss = { if (!viewModel.exchangeSubmitting) exchangeUnitScore = null },
            onConfirm = { score ->
                exchangeUnitScore = null
                scope.launch {
                    val result = viewModel.submitScoreExchange(score)
                    if (result.message.isNotEmpty()) showToast(result.message)
                }
            }
        )
    }
}

@Composable
private fun ScoreExchangeCard(
    wallets: List<WalletAccount>,
    activeWalletId: String,
    available: Int,
    submitting: Boolean,
    dynamicColor: Boolean,
    isLoggedIn: Boolean,
    onSelectWallet: (String) -> Unit,
    onRequestExchange: (Int) -> Unit
) {
    val activeWallet = wallets.firstOrNull { it.id == activeWalletId } ?: wallets.firstOrNull()
    val entries = remember(wallets, activeWallet?.id) {
        listOf(
            DropdownEntry(
                items = wallets.map { wallet ->
                    DropdownItem(
                        text = wallet.name.ifEmpty { "钱包" },
                        selected = wallet.id == activeWallet?.id,
                        onClick = { onSelectWallet(wallet.id) }
                    )
                }
            )
        )
    }
    var selectedAmount by remember { mutableStateOf<Int?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "可兑档位",
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (!isLoggedIn) {
                Text(
                    text = "请先登录",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            } else {
                OverlayDropdownMenu(
                    entries = entries,
                    title = activeWallet?.name?.ifEmpty { "钱包" } ?: "请选择钱包",
                    summary = if (wallets.size > 1) "点击切换钱包" else null,
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.BankCards,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    },
                    enabled = wallets.isNotEmpty()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SCORE_EXCHANGE_AMOUNTS.forEach { amount ->
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { selectedAmount = amount },
                            enabled = !submitting && available >= amount,
                            colors = if (selectedAmount == amount) {
                                ButtonDefaults.buttonColorsPrimary()
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Text(
                                text = "$amount 积分\n¥${formatTwoDecimals(amount / 1000.0)}",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedAmount?.let(onRequestExchange) },
                    enabled = selectedAmount != null && activeWallet != null &&
                        !submitting && available >= (selectedAmount ?: 0),
                    colors = primaryButtonColors(dynamicColor)
                ) {
                    Text(text = if (submitting) "兑换中…" else "立即兑换")
                }
            }
        }
    }
}

@Composable
private fun ScoreExchangeDialog(
    unitScore: Int,
    walletName: String,
    available: Int,
    appBlur: Boolean,
    dynamicColor: Boolean,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantityText by remember(unitScore) { mutableStateOf("1") }
    val quantity = quantityText.toIntOrNull()
    val maxQuantity = if (unitScore > 0) available / unitScore else 0
    val total = quantity?.takeIf { it in 1..maxQuantity }?.let { unitScore * it }

    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = "确认积分兑换",
        content = {
            WindowBlurEffect(useBlur = appBlur)
            val dismiss = LocalDismissState.current
            Column {
                Text(
                    text = "兑换到：$walletName",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                    text = "每份 $unitScore 积分 = ¥${formatTwoDecimals(unitScore / 1000.0)}",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { quantityText = ((quantity ?: 1) - 1).coerceAtLeast(1).toString() },
                        enabled = !submitting && quantity != null && quantity > 1,
                        text = "−"
                    )
                    TextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter { c -> c.isDigit() }.take(6) },
                        modifier = Modifier.weight(1f),
                        label = "份数"
                    )
                    TextButton(
                        onClick = { quantityText = ((quantity ?: 0) + 1).coerceAtMost(maxQuantity).toString() },
                        enabled = !submitting && quantity != null && quantity < maxQuantity,
                        text = "＋"
                    )
                }
                Text(
                    text = "可用积分：$available，最多 $maxQuantity 份",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = total?.let { "合计消耗 $it 积分，兑换 ¥${formatTwoDecimals(it / 1000.0)}" }
                        ?: "请输入 1～$maxQuantity 之间的整数份数",
                    style = MiuixTheme.textStyles.subtitle,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { dismiss?.invoke() },
                        enabled = !submitting,
                        text = "取消"
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { total?.let(onConfirm) },
                        enabled = total != null && !submitting,
                        text = if (submitting) "兑换中…" else "确认兑换",
                        colors = if (dynamicColor) {
                            ButtonDefaults.textButtonColorsPrimary()
                        } else {
                            ButtonDefaults.textButtonColors()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ScoreRecordsCard(
    records: List<ScoreRecord>,
    loadingMore: Boolean,
    hasMore: Boolean,
    isLoggedIn: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "积分记录",
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (records.isEmpty()) {
                Text(
                    text = if (!isLoggedIn) "请先登录" else "暂无数据",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            } else {
                records.forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.name,
                                style = MiuixTheme.textStyles.subtitle,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatScoreTime(record.time),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                        Text(
                            text = if (record.score > 0) "+${record.score}" else "${record.score}",
                            style = MiuixTheme.textStyles.title3,
                            fontWeight = FontWeight.Medium,
                            color = if (record.score > 0) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error
                        )
                    }
                }
                ScoreListFooter(
                    loadingMore = loadingMore,
                    hasMore = hasMore
                )
            }
        }
    }
}

@Composable
private fun ScoreSummaryItem(label: String, value: Int?, alignEnd: Boolean) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Text(
            text = value?.toString() ?: "-",
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface
        )
        Text(
            text = "≈${formatTwoDecimals((value ?: 0) / 1000.0)}元",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ScoreListFooter(loadingMore: Boolean, hasMore: Boolean) {
    when {
        loadingMore -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfiniteProgressIndicator(
                modifier = Modifier.size(18.dp),
                size = 18.dp,
                strokeWidth = 2.dp,
                orbitingDotSize = 3.dp
            )
        }

        !hasMore -> Text(
            text = "已加载全部记录",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

private fun formatScoreTime(time: String): String {
    if (time.isEmpty()) return ""
    val ts = time.toLongOrNull() ?: return time
    return formatTimestamp(ts, "yyyy-MM-dd HH:mm:ss")
}

private fun formatTwoDecimals(value: Double): String {
    val negative = value < 0
    val scaled = round(kotlin.math.abs(value) * 100).toLong()
    val intPart = scaled / 100
    val frac = (scaled % 100).toString().padStart(2, '0')
    return (if (negative) "-" else "") + "$intPart.$frac"
}

private fun scoreFilterLabel(filter: ScoreFilter): String = when (filter) {
    ScoreFilter.All -> "全部"
    ScoreFilter.Income -> "收入"
    ScoreFilter.Expense -> "支出"
}
