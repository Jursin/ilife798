package com.github.ilife798.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

// 打开赞助页：UIApplication.openURL；URL 为 commonMain 常量 SPONSOR_URL。
actual fun openSponsorPage(): Boolean =
    runCatching {
        UIApplication.sharedApplication.openURL(NSURL(string = SPONSOR_URL))
    }.getOrDefault(false)
