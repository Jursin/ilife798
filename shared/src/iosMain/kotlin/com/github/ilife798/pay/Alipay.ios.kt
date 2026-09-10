package com.github.ilife798.pay

actual suspend fun payWithAlipay(orderInfo: String): AlipayPayResult =
    AlipayPayResult(false, "", "iOS 暂不支持充值")
