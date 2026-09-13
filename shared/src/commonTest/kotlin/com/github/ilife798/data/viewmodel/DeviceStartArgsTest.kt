package com.github.ilife798.data.viewmodel

import com.github.ilife798.data.model.DeviceGoods
import com.github.ilife798.data.model.DeviceOption
import com.github.ilife798.data.model.DeviceStartOptions
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceStartArgsTest {
    @Test
    fun partsPreferredWithGoods() {
        val options =
            DeviceStartOptions(
                parts = listOf(DeviceOption(mode = 3, name = "标准")),
                goods = listOf(DeviceGoods(pos = 1, out = 2)),
            )
        assertEquals("""{"mode":3,"ext":[{"pos":1,"out":2}]}""", buildDeviceStartArgs(options, 0))
    }

    @Test
    fun channelWhenMultipleSubs() {
        assertEquals("""{"pos":1,"mode":null}""", buildDeviceStartArgs(DeviceStartOptions(subCount = 2), 1))
    }

    @Test
    fun emptyWhenNoOptions() {
        assertEquals("", buildDeviceStartArgs(DeviceStartOptions(), 0))
    }

    @Test
    fun emptyWhenPartIndexOutOfRange() {
        val options = DeviceStartOptions(parts = listOf(DeviceOption(mode = 3)))
        assertEquals("", buildDeviceStartArgs(options, 5))
    }
}
