package com.github.ilife798.ui.page.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceDetailRulesTest {
    @Test
    fun drinkingAndShowerDoNotRequireChannel() {
        assertFalse(isChannelRequired(dtype = 8, subsCount = 2, sensors = emptyList()))
        assertFalse(isChannelRequired(dtype = 6, subsCount = 1, sensors = emptyList()))
        assertFalse(isChannelRequired(dtype = 8, subsCount = 2, sensors = listOf(12)))
    }

    @Test
    fun chargingRequiresChannelWhenSubsExist() {
        assertTrue(isChannelRequired(dtype = 120, subsCount = 1, sensors = emptyList()))
        assertFalse(isChannelRequired(dtype = 120, subsCount = 0, sensors = emptyList()))
    }

    @Test
    fun hairDrierRequiresChannelOnlyWithMultipleSensor() {
        assertTrue(isChannelRequired(dtype = 20, subsCount = 2, sensors = listOf(4, 12), fmv = "1.0.312"))
        assertFalse(isChannelRequired(dtype = 20, subsCount = 2, sensors = listOf(4, 12), fmv = "1.0.311"))
        assertFalse(isChannelRequired(dtype = 20, subsCount = 2, sensors = listOf(4, 12), fmv = ""))
        assertFalse(isChannelRequired(dtype = 20, subsCount = 2, sensors = listOf(4, 12), fmv = "abc"))
        assertFalse(isChannelRequired(dtype = 20, subsCount = 2, sensors = listOf(4), fmv = "1.0.312"))
        assertFalse(isChannelRequired(dtype = 20, subsCount = 0, sensors = listOf(12), fmv = "1.0.312"))
    }

    @Test
    fun washingConfirmMessageFollowsGatewayType() {
        assertEquals(
            "请确认已将衣物放入洗衣机，确认选择标准洗模式",
            startConfirmMessage(10, "标准洗", gtype = 6),
        )
        assertEquals(
            "请确认是否将衣物放入洗衣机，点击确定之后，洗衣机将开始运行",
            startConfirmMessage(10, "标准洗", gtype = 2),
        )
        assertEquals(
            "请确认是否将衣物放入洗鞋机，点击确定之后，洗鞋机将开始运行",
            startConfirmMessage(90, "模式"),
        )
        assertEquals(
            "请确认选择的模式，点击确定之后，设备将开始运行",
            startConfirmMessage(8, "模式"),
        )
        assertEquals("确认开始充电？", startConfirmMessage(120, "模式"))
    }
}
