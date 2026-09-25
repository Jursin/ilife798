package com.github.ilife798.data.api

import com.github.ilife798.isIOS

// ApplicationType
const val APP_TYPE_DEVICE = "1,1" // 普通请求的固定值
const val APP_TYPE_POINTS = "1,5" // 积分登录（acc/login）签发积分 token，普通请求不携带

object ApiConfig {
    var baseUrl: String = ""
    var signSalt: String = ""
    var cid: String = ""
    const val VERSION_CODE: String = "3.1.9"
    val USER_AGENT: String = "${if (isIOS) "iOS" else "Android"}_ilife798_$VERSION_CODE"

    fun init(
        gateway: String,
        salt: String,
        clientId: String,
    ) {
        baseUrl = gateway
        signSalt = salt
        cid = clientId
    }
}
