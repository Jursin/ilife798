package com.github.ilife798

import platform.UIKit.UIPasteboard

actual fun copyTextToClipboard(text: String): Boolean {
    UIPasteboard.generalPasteboard.string = text
    return true
}
