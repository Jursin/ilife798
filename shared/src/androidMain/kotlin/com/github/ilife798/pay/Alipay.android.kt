package com.github.ilife798.pay

import com.alipay.sdk.app.PayTask
import com.github.ilife798.ActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun payWithAlipay(orderInfo: String): AlipayPayResult =
    withContext(Dispatchers.IO) {
        val activity =
            ActivityHolder.current
                ?: return@withContext AlipayPayResult(false, "无法获取当前页面，请重试")
        try {
            val result = PayTask(activity).payV2(orderInfo, true)
            val status = result["resultStatus"].orEmpty()
            val memo = result["memo"].orEmpty()
            when (status) {
                "9000" -> AlipayPayResult(true, "充值成功")
                "8000", "6004" -> AlipayPayResult(false, "支付结果确认中，请稍后刷新余额")
                "6001" -> AlipayPayResult(false, "已取消支付")
                else -> AlipayPayResult(false, if (memo.isBlank()) "支付失败" else "支付失败：$memo")
            }
        } catch (e: Exception) {
            AlipayPayResult(false, "支付失败：${e.message}")
        }
    }
