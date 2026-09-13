package com.github.ilife798

// 写入系统剪贴板，成功返回 true。
expect fun copyTextToClipboard(text: String): Boolean

// 复制文本并提示结果。
fun copyToClipboard(
    text: String,
    successMessage: String = "已复制",
) {
    showToast(if (copyTextToClipboard(text)) successMessage else "复制失败")
}
