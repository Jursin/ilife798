package com.github.ilife798.ui.page.bill

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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog
import com.github.ilife798.data.model.BillRecord
import com.github.ilife798.data.model.RechargeProduct
import com.github.ilife798.data.model.RefundProgress
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
import kotlin.math.abs

private const val LOAD_MORE_THRESHOLD_PX = 200

private enum class BillPanel { Records, Recharge, Refund }

@Composable
fun MyBillPage(viewModel: AppViewModel, onBack: () -> Unit) {
    val state = viewModel.state
    val wallets = state.wallets
    val activeWallet = wallets.firstOrNull { it.id == state.activeWalletId } ?: wallets.firstOrNull()
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)
    val scrollState = rememberScrollState()
    var panel by remember { mutableStateOf(BillPanel.Records) }
    val scope = rememberCoroutineScope()
    var selectedProductId by remember { mutableStateOf<String?>(null) }
    var showRefundDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (state.account.appToken.isNotEmpty() || state.account.token.isNotEmpty()) {
            viewModel.refreshBillPage()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val max = scrollState.maxValue
            max > 0 && scrollState.value >= max - LOAD_MORE_THRESHOLD_PX
        }
    }
    LaunchedEffect(shouldLoadMore, state.billRecords.size) {
        if (shouldLoadMore && panel == BillPanel.Records) viewModel.loadMoreBills()
    }
    LaunchedEffect(panel) {
        scrollState.animateScrollTo(0)
        if (panel == BillPanel.Recharge) {
            selectedProductId = null
            viewModel.loadRechargeProducts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "我的账单",
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
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WalletHeaderCard(
                wallets = wallets,
                activeWallet = activeWallet,
                panel = panel,
                onSelectWallet = { viewModel.selectWallet(it) },
                onToggleRecharge = {
                    panel = if (panel == BillPanel.Recharge) BillPanel.Records else BillPanel.Recharge
                },
                onToggleRefund = {
                    panel = if (panel == BillPanel.Refund) BillPanel.Records else BillPanel.Refund
                }
            )

            AnimatedContent(
                targetState = panel,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 10 }) togetherWith
                        (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 10 })
                },
                label = "billPanel"
            ) { target ->
                when (target) {
                    BillPanel.Records -> BillRecordsCard(
                        records = state.billRecords,
                        loadingMore = viewModel.billLoadingMore,
                        hasMore = viewModel.billHasMore
                    )

                    BillPanel.Recharge -> RechargeCard(
                        products = viewModel.rechargeProducts,
                        loading = viewModel.rechargeLoading,
                        paying = viewModel.rechargePaying,
                        selectedProductId = selectedProductId,
                        dynamicColor = state.dynamicColor,
                        onSelectProduct = { selectedProductId = it },
                        onRecharge = {
                            val product = viewModel.rechargeProducts
                                .firstOrNull { it.id == selectedProductId }
                            if (product != null) {
                                scope.launch {
                                    val result = viewModel.submitRecharge(product)
                                    showToast(result.message)
                                }
                            }
                        }
                    )

                    BillPanel.Refund -> RefundCard(
                        wallet = activeWallet,
                        refundProgress = viewModel.refundProgress,
                        paying = viewModel.refundSubmitting,
                        onRefund = { showRefundDialog = true }
                    )
                }
            }
        }
    }

    if (showRefundDialog) {
        RefundConfirmDialog(
            wallet = activeWallet,
            appBlur = state.appBlur,
            paying = viewModel.refundSubmitting,
            onDismiss = { if (!viewModel.refundSubmitting) showRefundDialog = false },
            onConfirm = {
                scope.launch {
                    val result = viewModel.submitRefund()
                    if (result.success) showRefundDialog = false
                    showToast(
                        result.message.ifEmpty {
                            if (result.success) "退款申请已提交" else "退款失败"
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun RefundConfirmDialog(
    wallet: WalletAccount?,
    appBlur: Boolean,
    paying: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = "确认退款",
        content = {
            WindowBlurEffect(useBlur = appBlur)
            val dismiss = LocalDismissState.current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "可退金额 ¥${formatMoney(wallet?.refundable)}",
                    style = MiuixTheme.textStyles.title3,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "余额将退款至支付宝账号，赠送部分清零，提交后需商家审核。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { dismiss?.invoke() },
                        enabled = !paying,
                        text = "取消"
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        enabled = !paying,
                        text = if (paying) "提交中…" else "确认退款",
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

@Composable
private fun WalletHeaderCard(
    wallets: List<WalletAccount>,
    activeWallet: WalletAccount?,
    panel: BillPanel,
    onSelectWallet: (String) -> Unit,
    onToggleRecharge: () -> Unit,
    onToggleRefund: () -> Unit
) {
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            OverlayDropdownMenu(
                entries = entries,
                title = activeWallet?.name?.ifEmpty { "钱包" } ?: "钱包",
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
            HorizontalDivider()
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "总余额",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Text(
                    text = "¥${formatMoney(activeWallet?.total)}",
                    style = MiuixTheme.textStyles.title1,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onToggleRecharge,
                        colors = if (panel == BillPanel.Recharge) {
                            ButtonDefaults.buttonColorsPrimary()
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(text = "充值")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onToggleRefund,
                        colors = if (panel == BillPanel.Refund) {
                            ButtonDefaults.buttonColorsPrimary()
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(text = "退款")
                    }
                }
            }
        }
    }
}

@Composable
private fun BillRecordsCard(
    records: List<BillRecord>,
    loadingMore: Boolean,
    hasMore: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "账单记录",
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (records.isEmpty()) {
                Text(
                    text = "暂无数据",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            } else {
                records.forEach { record ->
                    BillRecordRow(record)
                }
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
        }
    }
}

@Composable
private fun BillRecordRow(record: BillRecord) {
    val isRefund = record.dir == 2
    val amountColor = if (isRefund) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
    val statusColor = when {
        isRefund -> if (record.status == 3) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error
        record.status == 3 -> MiuixTheme.colorScheme.primary
        record.status == 4 -> MiuixTheme.colorScheme.error
        record.status == 1 -> MiuixTheme.colorScheme.error
        else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.msg.ifEmpty { "账单记录" },
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface
            )
            Text(
                text = formatBillTime(record.time),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = billStatusName(record.status, record.dir),
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
                Text(
                    text = "·",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Text(
                    text = paymentTypeName(record.type),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
        Text(
            text = (if (isRefund) "+¥" else "¥") + formatMoney(record.payment),
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Medium,
            color = amountColor
        )
    }
}

@Composable
private fun RechargeCard(
    products: List<RechargeProduct>,
    loading: Boolean,
    paying: Boolean,
    selectedProductId: String?,
    dynamicColor: Boolean,
    onSelectProduct: (String) -> Unit,
    onRecharge: () -> Unit
) {
    val selectedProduct = products.firstOrNull { it.id == selectedProductId }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "充值金额",
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            when {
                loading && products.isEmpty() -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfiniteProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        size = 20.dp,
                        strokeWidth = 2.dp,
                        orbitingDotSize = 3.dp
                    )
                }

                products.isEmpty() -> Text(
                    text = "暂无可充值金额",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )

                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    products.chunked(3).forEach { rowProducts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowProducts.forEach { product ->
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectProduct(product.id) },
                                    enabled = !paying,
                                    colors = if (product.id == selectedProductId) {
                                        ButtonDefaults.buttonColorsPrimary()
                                    } else {
                                        ButtonDefaults.buttonColors()
                                    }
                                ) {
                                    Text(text = "¥${formatMoney(product.price)}")
                                }
                            }
                            repeat(3 - rowProducts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRecharge,
                enabled = selectedProduct != null && !paying,
                colors = primaryButtonColors(dynamicColor)
            ) {
                Text(text = if (paying) "充值中…" else "立即充值")
            }
        }
    }
}

@Composable
private fun RefundCard(
    wallet: WalletAccount?,
    refundProgress: RefundProgress?,
    paying: Boolean,
    onRefund: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "可退余额",
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "¥${formatMoney(wallet?.refundable)}",
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.error
            )
            Text(
                text = "赠送余额不支持退款",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            BalanceBreakdown(wallet)
            refundProgress?.takeIf { it.active }?.let { progress ->
                val statusText = when (progress.fail) {
                    1 -> "退款失败"
                    0 -> "退款成功"
                    else -> "退款审核中"
                }
                val statusColor = when (progress.fail) {
                    1 -> MiuixTheme.colorScheme.error
                    0 -> MiuixTheme.colorScheme.primary
                    else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                }
                Text(
                    text = "$statusText：${progress.count}笔 ¥${formatMoney(progress.total)}元",
                    style = MiuixTheme.textStyles.body2,
                    color = statusColor,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRefund,
                enabled = (wallet?.refundEnabled ?: true) && (wallet?.refundable ?: 0.0) > 0.0 && !paying
            ) {
                Text(text = if (paying) "提交中…" else "立即退款")
            }
        }
    }
}

@Composable
private fun BalanceBreakdown(wallet: WalletAccount?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BalanceText(label = "线上余额", value = wallet?.olCash, modifier = Modifier.weight(1f))
            BalanceText(label = "线下余额", value = wallet?.ofCash, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BalanceText(label = "线上赠送", value = wallet?.olGift, modifier = Modifier.weight(1f))
            BalanceText(label = "线下赠送", value = wallet?.ofGift, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BalanceText(label: String, value: Double?, modifier: Modifier) {
    Text(
        text = "$label：${formatMoney(value)}元",
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier
    )
}

private fun formatBillTime(time: Long): String {
    if (time <= 0L) return ""
    return formatTimestamp(time, "yyyy-MM-dd HH:mm:ss")
}

private fun paymentTypeName(type: Int): String = when (type) {
    11 -> "微信（APP）"
    12 -> "微信（公众号）"
    13 -> "微信（小程序）"
    21 -> "支付宝（APP）"
    22 -> "支付宝（生活号）"
    23 -> "支付宝（小程序）"
    24 -> "支付宝（扫脸）"
    31 -> "翼支付"
    41 -> "云闪付（APP）"
    43 -> "云闪付（小程序）"
    51 -> "招商银行"
    52 -> "农行支付"
    91 -> "商家钱包"
    92 -> "余额"
    93 -> "一卡通"
    else -> "其他"
}

private fun billStatusName(status: Int, dir: Int): String {
    if (dir == 2) return if (status == 3) "已退款" else "退款中"
    return when (status) {
        1 -> "未付款"
        2 -> "待确认"
        3 -> "已付款"
        4 -> "付款失败"
        5 -> "核算中"
        9 -> "已取消"
        else -> "未知状态"
    }
}

private fun formatMoney(value: Double?): String {
    val v = value ?: 0.0
    val negative = v < 0
    val scaled = round(abs(v) * 100).toLong()
    val intPart = scaled / 100
    val frac = (scaled % 100).toString().padStart(2, '0')
    return (if (negative) "-" else "") + "$intPart.$frac"
}
