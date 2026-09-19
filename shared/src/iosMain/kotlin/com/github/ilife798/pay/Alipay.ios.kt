package com.github.ilife798.pay

import com.github.ilife798.logDebug
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication

// iOS 支付：支付宝 URL Scheme 跳转。
// 复用后端给 Android SDK 的同一份签名 orderInfo，零后端改动；iOS 无法自动回查支付结果，
// 成功唤起时返回语义与 Android 8000/6004 分支一致（"支付结果确认中，请稍后刷新余额"）。
// 结果文案经 BillPage 的 showToast 展示——iOS Toast 当前为静默 no-op（Change 1 骨架边界），
// 故以 logDebug 记录分支供模拟器/真机验证观测。
actual suspend fun payWithAlipay(orderInfo: String): AlipayPayResult {
    val application = UIApplication.sharedApplication
    val schemeUrl = NSURL(string = ALIPAY_SCHEME)
    // LSApplicationQueriesSchemes 已声明 alipays/alipay，canOpenURL 才可判安装
    val installed =
        schemeUrl != null &&
            runCatching { application.canOpenURL(schemeUrl) }.getOrDefault(false)
    if (!installed) {
        logDebug("Alipay", "alipay not installed")
        return AlipayPayResult(success = false, message = "未安装支付宝")
    }
    // orderInfo 含 & = % + 等保留字符：按「非字母数字全量编码」保证 orderSuffix 完整不截断
    val encoded =
        (orderInfo as NSString)
            .stringByAddingPercentEncodingWithAllowedCharacters(
                NSCharacterSet.alphanumericCharacterSet.invertedSet(),
            ) ?: return AlipayPayResult(success = false, message = "支付失败：订单信息编码失败")
    val encodedText = encoded as String
    val jumpUrl =
        NSURL(string = "$ALIPAY_STARTAPP_URL?appId=20000125&orderSuffix=$encodedText")
            ?: return AlipayPayResult(success = false, message = "支付失败：跳转地址拼装失败")
    val opened = runCatching { application.openURL(jumpUrl) }.getOrDefault(false)
    return if (opened) {
        logDebug("Alipay", "jumped to alipay, result pending")
        AlipayPayResult(success = false, message = "已跳转支付宝，支付结果确认中，请稍后刷新余额")
    } else {
        logDebug("Alipay", "openURL failed")
        AlipayPayResult(success = false, message = "支付失败：无法唤起支付宝，请手动打开支付宝完成支付")
    }
}

private const val ALIPAY_SCHEME = "alipays://"
private const val ALIPAY_STARTAPP_URL = "alipays://platformapi/startapp"
