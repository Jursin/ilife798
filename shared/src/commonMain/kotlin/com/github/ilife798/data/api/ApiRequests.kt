package com.github.ilife798.data.api

import kotlinx.serialization.Serializable

private const val MISSION_EXEC_TYPE = 101
private const val REFUND_TYPE = 23
private const val EXCHANGE_TYPE = 1
private const val RECHARGE_CATA = 1
private const val RECHARGE_NOTE = "钱包充值"

@Serializable
data class SmsCodeRequest(
    val un: String,
    val authCode: String,
    val s: String,
)

@Serializable
data class LoginRequest(
    val openCode: String = "",
    val authCode: String,
    val un: String,
    val cid: String,
)

@Serializable
data class SetUseScoreRequest(
    val useScore: Int,
)

@Serializable
data class MissionExecRequest(
    val adId: String,
    val type: Int = MISSION_EXEC_TYPE,
)

@Serializable
data class SignInRequest(
    val weekDay: Int,
    val adId: String,
)

@Serializable
data class RefundWalletRequest(
    val eid: String,
    val type: Int = REFUND_TYPE,
)

@Serializable
data class ExchangeScoreRequest(
    val ep: EndpointRef,
    val score: Int,
    val type: Int = EXCHANGE_TYPE,
)

@Serializable
data class EndpointRef(
    val id: String,
)

@Serializable
data class ProductRef(
    val id: String,
    val count: Int,
)

@Serializable
data class CreateRechargeOrderRequest(
    val cata: Int = RECHARGE_CATA,
    val contact: EndpointRef,
    val ep: EndpointRef,
    val note: String = RECHARGE_NOTE,
    val owner: EndpointRef,
    val prds: List<ProductRef>,
)
