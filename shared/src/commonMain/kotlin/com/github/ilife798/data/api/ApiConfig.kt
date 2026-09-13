package com.github.ilife798.data.api

// ApplicationType：设备控制使用设备登录凭据，积分/钱包使用积分登录凭据。
const val APP_TYPE_DEVICE = "1,1"
const val APP_TYPE_POINTS = "1,5"

object ApiConfig {
    var baseUrl: String = ""
    var signSalt: String = ""
    var cid: String = ""
    const val USER_AGENT: String = "Android_ilife798_3.1.7"
    const val VERSION_CODE: String = "3.1.7"

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
