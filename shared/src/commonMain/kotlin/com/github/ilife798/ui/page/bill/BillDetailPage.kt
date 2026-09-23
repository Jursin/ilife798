package com.github.ilife798.ui.page.bill

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.ilife798.copyToClipboard
import com.github.ilife798.data.model.BillDetailInfo
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.shared.resources.Res
import com.github.ilife798.shared.resources.bill_cleaning
import com.github.ilife798.shared.resources.bill_default
import com.github.ilife798.shared.resources.bill_recharge
import com.github.ilife798.shared.resources.bill_refund
import com.github.ilife798.shared.resources.bill_vip
import com.github.ilife798.ui.component.BlurredTopAppBar
import com.github.ilife798.ui.component.DeviceIcon
import com.github.ilife798.ui.component.EmptyStateText
import com.github.ilife798.ui.component.InfoRow
import com.github.ilife798.ui.component.PageScrollColumn
import com.github.ilife798.ui.component.SectionHeader
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.github.ilife798.util.formatDateTime
import com.github.ilife798.util.formatMoney
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 账单详情：数据来自 bill/view-full
@Composable
fun BillDetailPage(
    viewModel: AppViewModel,
    billId: String,
    onBack: () -> Unit,
) {
    val state = viewModel.state
    val isLoggedIn = state.account.hasAnyToken
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)
    var detail by remember(billId) { mutableStateOf<BillDetailInfo?>(null) }
    var loaded by remember(billId) { mutableStateOf(false) }

    LaunchedEffect(billId) {
        if (isLoggedIn) {
            detail = viewModel.getBillDetail(billId)
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            BlurredTopAppBar(
                title = "账单详情",
                blurBackdrop = blurBackdrop,
                scrollBehavior = scrollBehavior,
                onBack = onBack,
            )
        },
    ) { paddingValues ->
        PageScrollColumn(blurBackdrop, scrollBehavior, paddingValues) {
            val bill = detail
            when {
                !isLoggedIn -> {
                    EmptyStateText(isLoggedIn = false)
                }

                bill == null && !loaded -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader("账单信息")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                InfiniteProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    size = 20.dp,
                                    strokeWidth = 2.dp,
                                    orbitingDotSize = 3.dp,
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "正在加载账单信息…",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }

                bill == null -> {
                    EmptyStateText(isLoggedIn = true, emptyText = "账单加载失败，请稍后重试")
                }

                else -> {
                    BillHeaderCard(
                        bill,
                        onCopyDeviceId = { copyToClipboard(bill.deviceId, "已复制设备编号") },
                    )
                    BillInfoCard(
                        bill,
                        onCopyBillId = { copyToClipboard(bill.id, "已复制账单编号") },
                    )
                }
            }
        }
    }
}

@Composable
private fun BillHeaderCard(
    bill: BillDetailInfo,
    onCopyDeviceId: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 设备消费→设备图标；充值类→钱包；VIP/E袋洗→专属；退款→退款；兜底默认
                if (bill.cata == 6) {
                    DeviceIcon(dtype = bill.deviceDtype, size = 40.dp)
                } else {
                    val iconRes =
                        when {
                            bill.cata == 1 || bill.cata == 7 -> Res.drawable.bill_recharge
                            bill.cata == 9 -> Res.drawable.bill_vip
                            bill.cata == 3 -> Res.drawable.bill_cleaning
                            bill.dir == 2 -> Res.drawable.bill_refund
                            else -> Res.drawable.bill_default
                        }
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = bill.deviceName.ifEmpty { bill.msg.ifEmpty { "账单详情" } },
                            style = MiuixTheme.textStyles.title3,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = billStatusName(bill.status, bill.dir),
                            style = MiuixTheme.textStyles.body2,
                            fontWeight = FontWeight.Medium,
                            color = billStatusColor(bill.status, bill.dir),
                        )
                    }
                    if (bill.deviceId.isNotEmpty()) {
                        Text(
                            text = bill.deviceId,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier =
                                Modifier
                                    .padding(top = 2.dp)
                                    .clickable(onClick = onCopyDeviceId),
                        )
                    }
                }
            }
            if (bill.discount > 0) {
                InfoRow("原价", "¥${formatMoney(bill.payment + bill.discount)}")
                InfoRow("折扣", "-¥${formatMoney(bill.discount)}")
            }
            // 有券显示券名；未付款显示可用张数；其余隐藏
            val showPromo = bill.promoName.isNotEmpty() || bill.status == 1
            if (showPromo) {
                val promoValue =
                    when {
                        bill.promoName.isNotEmpty() -> bill.promoName
                        bill.couponCount > 0 -> "${bill.couponCount} 张可用"
                        else -> "暂无可用"
                    }
                InfoRow("优惠券", promoValue)
            }
            InfoRow(
                "合计",
                (if (bill.dir == 2) "+" else "") + "¥" + formatMoney(bill.payment),
            )
        }
    }
}

@Composable
private fun BillInfoCard(
    bill: BillDetailInfo,
    onCopyBillId: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("账单信息")
            InfoRow("账单编号", bill.id, onValueClick = onCopyBillId)
            if (bill.enterpriseName.isNotEmpty()) {
                InfoRow("商家名称", bill.enterpriseName)
            }
            if (bill.msg.isNotEmpty()) {
                InfoRow("交易描述", bill.msg)
            }
            InfoRow("产品类型", productTypeName(bill.cata))
            InfoRow("支付方式", paymentTypeName(bill.type))
            InfoRow("创建时间", formatDateTime(bill.ctime))
            if (bill.utime > 0) {
                InfoRow("支付时间", formatDateTime(bill.utime))
            }
        }
    }
}
