package com.github.ilife798.data.api

import com.github.ilife798.util.currentTimeMillis
import com.github.ilife798.util.md5

object Signer {
    fun sign(
        adId: String,
        token: String,
        uid: String,
    ): String {
        val timeBucket = 10 * (currentTimeMillis() / 10000)
        val tokenTail = if (token.length >= 8) token.substring(token.length - 8) else token
        val uidTail = if (uid.length >= 8) uid.substring(uid.length - 8) else uid
        val raw = "$adId${timeBucket}${tokenTail}${uidTail}${ApiConfig.signSalt}"
        return md5(raw)
    }
}
