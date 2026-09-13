package com.github.ilife798.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

const val LOAD_MORE_THRESHOLD_PX = 200

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    InfiniteProgressIndicator(
        modifier = modifier.size(18.dp),
        size = 18.dp,
        strokeWidth = 2.dp,
        orbitingDotSize = 3.dp,
    )
}

// 分页列表底部：加载中转圈，全部加载完显示提示。
@Composable
fun ListLoadMoreFooter(
    loadingMore: Boolean,
    hasMore: Boolean,
) {
    when {
        loadingMore -> {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoadingSpinner()
            }
        }

        !hasMore -> {
            Text(
                text = "已加载全部记录",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
            )
        }
    }
}
