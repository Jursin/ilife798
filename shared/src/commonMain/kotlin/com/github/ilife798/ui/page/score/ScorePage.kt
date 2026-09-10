package com.github.ilife798.ui.page.score

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.github.ilife798.ui.theme.captureForBlur
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.ui.theme.appBarBlur
import com.github.ilife798.ui.theme.blurAppBarColor
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.github.ilife798.util.formatTimestamp
import kotlin.math.round

private const val LOAD_MORE_THRESHOLD_PX = 200

@Composable
fun ScorePage(viewModel: AppViewModel, onBack: () -> Unit) {
    val state = viewModel.state
    val scores = state.scoreRecords
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (state.account.token.isNotEmpty()) {
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
        if (shouldLoadMore) viewModel.loadMoreScores()
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
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "积分记录",
                        style = MiuixTheme.textStyles.title2,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    if (scores.isEmpty()) {
                        Text(
                            text = "暂无记录",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    } else {
                        scores.forEach { record ->
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
                            loadingMore = viewModel.scoreLoadingMore,
                            hasMore = viewModel.scoreHasMore
                        )
                    }
                }
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
