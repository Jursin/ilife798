package com.github.ilife798.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 返回系统主题色，作为动态取色的种子色；
// 平台未提供时返回 null，回退到 Miuix 自带的平台动态色。
@Composable
expect fun systemDynamicColorKey(): Color?
