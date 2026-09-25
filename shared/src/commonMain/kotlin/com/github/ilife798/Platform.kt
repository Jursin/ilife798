package com.github.ilife798

// 是否为 iOS 平台。
expect val isIOS: Boolean

// 是否支持预测性返回动画（Android 13+）。
expect val supportsPredictiveBack: Boolean

// 是否支持基于系统壁纸的动态取色（Android 12+）。
expect val supportsDynamicColor: Boolean
