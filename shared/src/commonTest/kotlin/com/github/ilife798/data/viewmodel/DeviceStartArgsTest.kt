package com.github.ilife798.data.viewmodel

import com.github.ilife798.data.model.DeviceGoods
import com.github.ilife798.data.model.DeviceOption
import com.github.ilife798.data.model.DeviceStartOptions
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceStartArgsTest {
    @Test
    fun detailPassageDeviceWithModeAndChannel() {
        val options = DeviceStartOptions(parts = listOf(DeviceOption(mode = 7)))
        assertEquals(
            """{"pos":2,"mode":7}""",
            buildDeviceDetailStartArgs(options, partMode = 7, channelIndex = 2, passageDevice = true),
        )
    }

    @Test
    fun detailPassageDeviceWithoutChannelReturnsEmpty() {
        assertEquals(
            "",
            buildDeviceDetailStartArgs(DeviceStartOptions(), partMode = 7, channelIndex = -1, passageDevice = true),
        )
    }

    @Test
    fun detailModePreferredWithGoods() {
        val options = DeviceStartOptions(goods = listOf(DeviceGoods(pos = 1, out = 2)))
        assertEquals(
            """{"mode":3,"ext":[{"pos":1,"out":2}]}""",
            buildDeviceDetailStartArgs(options, partMode = 3, channelIndex = -1, passageDevice = false),
        )
    }

    @Test
    fun detailChannelOnly() {
        assertEquals(
            """{"pos":1,"mode":null}""",
            buildDeviceDetailStartArgs(DeviceStartOptions(subCount = 2), partMode = null, channelIndex = 1, passageDevice = false),
        )
    }

    @Test
    fun detailEmptyWhenNoSelection() {
        assertEquals(
            "",
            buildDeviceDetailStartArgs(DeviceStartOptions(), partMode = null, channelIndex = -1, passageDevice = false),
        )
    }
}
