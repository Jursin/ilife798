package com.github.ilife798.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class WalletAccountTest {
    @Test
    fun refundableExcludesOfflineCashWhenUnauthorized() {
        val wallet = WalletAccount(olCash = 10.0, ofCash = 5.0, olGift = 100.0, ofGift = 50.0, auth = false)
        assertEquals(10.0, wallet.refundable)
    }

    @Test
    fun refundableIncludesOfflineCashWhenAuthorized() {
        val wallet = WalletAccount(olCash = 10.0, ofCash = 5.0, olGift = 100.0, ofGift = 50.0, auth = true)
        assertEquals(15.0, wallet.refundable)
    }
}
