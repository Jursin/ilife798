package com.github.ilife798.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Returns the system accent color used as the seed for dynamic color schemes,
 * or null when the platform does not provide one (falls back to Miuix's own
 * platform dynamic colors).
 */
@Composable
expect fun systemDynamicColorKey(): Color?
