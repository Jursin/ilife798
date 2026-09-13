package com.github.ilife798.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelsTest {
    @Test
    fun homeDeviceTypeFromDeviceType() {
        assertEquals(HomeDeviceType.Laundry, HomeDeviceType.fromDeviceType(10))
        assertEquals(HomeDeviceType.HairDrying, HomeDeviceType.fromDeviceType(20))
        assertEquals(HomeDeviceType.Drinking, HomeDeviceType.fromDeviceType(8))
        assertEquals(HomeDeviceType.Shower, HomeDeviceType.fromDeviceType(6))
        assertNull(HomeDeviceType.fromDeviceType(0))
        assertNull(HomeDeviceType.fromDeviceType(999))
    }

    @Test
    fun accountPreferredTokenAndHasAnyToken() {
        val withApp = Account(appToken = "a", token = "b")
        assertEquals("a", withApp.preferredToken)
        assertTrue(withApp.hasAnyToken)

        val onlyPoints = Account(token = "b")
        assertEquals("b", onlyPoints.preferredToken)
        assertTrue(onlyPoints.hasAnyToken)

        val empty = Account()
        assertEquals("", empty.preferredToken)
        assertFalse(empty.hasAnyToken)
    }

    @Test
    fun refundProgressActive() {
        assertFalse(RefundProgress(ctime = -1L).active)
        assertTrue(RefundProgress(ctime = 123L).active)
    }
}
