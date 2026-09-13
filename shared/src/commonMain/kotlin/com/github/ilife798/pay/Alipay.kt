package com.github.ilife798.pay

data class AlipayPayResult(
    val success: Boolean,
    val message: String,
)

expect suspend fun payWithAlipay(orderInfo: String): AlipayPayResult
