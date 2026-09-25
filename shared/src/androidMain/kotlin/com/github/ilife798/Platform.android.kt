package com.github.ilife798

import android.os.Build

actual val isIOS: Boolean = false

actual val supportsPredictiveBack: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

actual val supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
