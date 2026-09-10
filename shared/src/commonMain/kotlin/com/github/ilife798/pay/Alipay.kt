package com.github.ilife798.pay

data class AlipayPayResult(
    val success: Boolean,
    val status: String,
    val message: String
) {
    val processing: Boolean get() = status == "8000" || status == "6004"
    val cancelled: Boolean get() = status == "6001"
}

expect suspend fun payWithAlipay(orderInfo: String): AlipayPayResult
