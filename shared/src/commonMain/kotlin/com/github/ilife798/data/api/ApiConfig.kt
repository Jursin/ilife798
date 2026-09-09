package com.github.ilife798.data.api

object ApiConfig {
    var baseUrl: String = ""
    var signSalt: String = ""
    var cid: String = ""
    const val USER_AGENT: String = "Android_ilife798_3.1.7"
    const val VERSION_CODE: String = "3.1.7"

    fun init(gateway: String, salt: String, clientId: String) {
        baseUrl = gateway
        signSalt = salt
        cid = clientId
    }
}
