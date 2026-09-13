package com.github.ilife798.ui.page.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.ilife798.copyToClipboard
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.ui.component.AppPullToRefresh
import com.github.ilife798.ui.component.BlurredTopAppBar
import com.github.ilife798.ui.component.ConfirmDialog
import com.github.ilife798.ui.component.EmptyStateText
import com.github.ilife798.ui.component.LoadingSpinner
import com.github.ilife798.ui.component.PageScrollColumn
import com.github.ilife798.ui.component.SectionHeader
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.github.ilife798.util.getDayOfWeek
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TasksPage(
    viewModel: AppViewModel,
    onLoginClick: (isAlipay: Boolean) -> Unit = {},
    bottomPadding: Dp,
) {
    val state = viewModel.state
    val missions = viewModel.missions
    val isLoading = viewModel.isLoading
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)
    val isLoggedIn = state.account.hasAnyToken
    var showStopDialog by remember { mutableStateOf(false) }

    LaunchedEffect(missions) {
        if (missions.isNotEmpty() && !viewModel.isLoading && !state.taskCompleted) {
            val allDone =
                missions
                    .filter { !it.isDailySignin && it.limit > 0 && it.score > 0 }
                    .all { it.dailyCompleted >= it.limit }
            val signInDone =
                missions.firstOrNull { it.isDailySignin }?.let {
                    val weekDay = getDayOfWeek()
                    (state.weekMask and (1 shl (weekDay - 1))) != 0
                } ?: true
            if (allDone && signInDone) {
                viewModel.setTaskCompleted()
            }
        }
    }

    Scaffold(
        topBar = { BlurredTopAppBar("积分任务", blurBackdrop, scrollBehavior) },
    ) { paddingValues ->
        AppPullToRefresh(viewModel.tasksRefreshing, { viewModel.refreshTasks() }, scrollBehavior, paddingValues) {
            PageScrollColumn(
                blurBackdrop = blurBackdrop,
                scrollBehavior = scrollBehavior,
                contentPadding = paddingValues,
                bottomPadding = bottomPadding,
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (isLoading) showStopDialog = true else viewModel.runAllTasks()
                    },
                    enabled = isLoading || (isLoggedIn && !state.taskCompleted),
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LoadingSpinner()
                            Text(text = "运行中，点击停止")
                        }
                    } else {
                        Text(text = if (state.taskCompleted) "今日任务已完成" else "运行积分任务")
                    }
                }

                if (missions.isNotEmpty()) {
                    val signIn = missions.firstOrNull { it.isDailySignin }
                    val regularMissions = missions.filter { !it.isDailySignin && it.limit > 0 && it.score > 0 }
                    val allMissions = listOfNotNull(signIn) + regularMissions

                    if (allMissions.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader("任务列表 (${allMissions.size})")
                                allMissions.forEach { mission ->
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mission.name,
                                                style = MiuixTheme.textStyles.body2,
                                                color = MiuixTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text =
                                                    if (mission.isDailySignin) {
                                                        "每次 +${mission.score} 分"
                                                    } else {
                                                        "上限 ${mission.limit} 次 · 每次 +${mission.score} 分"
                                                    },
                                                style = MiuixTheme.textStyles.body2,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                        if (mission.isDailySignin) {
                                            val weekDay = getDayOfWeek()
                                            val alreadySigned = (state.weekMask and (1 shl (weekDay - 1))) != 0
                                            if (alreadySigned) {
                                                Text(
                                                    text = "已签到",
                                                    style = MiuixTheme.textStyles.body2,
                                                    color = MiuixTheme.colorScheme.primary,
                                                )
                                            }
                                        } else if (mission.dailyCompleted > 0) {
                                            Text(
                                                text = "已完成（${mission.dailyCompleted}/${mission.limit}）",
                                                style = MiuixTheme.textStyles.body2,
                                                color =
                                                    if (mission.dailyCompleted >= mission.limit) {
                                                        MiuixTheme.colorScheme.primary
                                                    } else {
                                                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                                                    },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader("任务列表")
                            EmptyStateText(isLoggedIn)
                        }
                    }
                }

                if (state.account.appToken.isNotEmpty() && !state.account.pointsLoginDone && state.taskLogs.isEmpty()) {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onLoginClick(true) },
                        colors =
                            CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                contentColor = MiuixTheme.colorScheme.onSurface,
                            ),
                    ) {
                        Text(
                            text = "完成积分登录解锁更多任务。",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                if (state.taskLogs.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "运行日志",
                                    style = MiuixTheme.textStyles.title2,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                IconButton(
                                    minHeight = 35.dp,
                                    minWidth = 35.dp,
                                    backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                    onClick = {
                                        copyToClipboard(state.taskLogs.joinToString("\n"), "已复制到剪贴板")
                                    },
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Copy,
                                        contentDescription = "复制日志",
                                        modifier = Modifier.size(20.dp),
                                        tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            state.taskLogs.forEach { log ->
                                Text(
                                    text = log,
                                    style = MiuixTheme.textStyles.body2,
                                    color =
                                        if (log.contains("成功")) {
                                            MiuixTheme.colorScheme.primary
                                        } else if (log.contains("失败") || log.contains("异常")) {
                                            MiuixTheme.colorScheme.error
                                        } else {
                                            MiuixTheme.colorScheme.onSurface
                                        },
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStopDialog) {
        ConfirmDialog(
            title = "停止任务",
            message = "确定要停止正在运行的积分任务吗？",
            appBlur = state.appBlur,
            confirmText = "停止",
            onConfirm = {
                viewModel.stopTasks()
                showStopDialog = false
            },
            onDismiss = { showStopDialog = false },
        )
    }
}
