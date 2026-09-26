package com.github.ilife798.data.viewmodel

import com.github.ilife798.data.model.DeviceGoods
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceStartArgsTest {
    @Test
    fun detailPassageDeviceWithModeAndChannel() {
        assertEquals(
            """{"pos":2,"mode":7}""",
            buildDeviceDetailStartArgs(emptyList(), partMode = 7, channelIndex = 2, passageDevice = true),
        )
    }

    @Test
    fun detailPassageDeviceWithoutModeUsesNull() {
        assertEquals(
            """{"pos":1,"mode":null}""",
            buildDeviceDetailStartArgs(emptyList(), partMode = null, channelIndex = 1, passageDevice = true),
        )
    }

    @Test
    fun detailPassageDeviceWithoutChannelReturnsEmpty() {
        assertEquals(
            "",
            buildDeviceDetailStartArgs(emptyList(), partMode = 7, channelIndex = -1, passageDevice = true),
        )
    }

    @Test
    fun detailModePreferredWithGoods() {
        val goods = listOf(DeviceGoods(pos = 1, out = 2))
        assertEquals(
            """{"mode":3,"ext":[{"pos":1,"out":2}]}""",
            buildDeviceDetailStartArgs(goods, partMode = 3, channelIndex = -1, passageDevice = false),
        )
    }

    @Test
    fun detailChannelOnly() {
        assertEquals(
            """{"pos":1,"mode":null}""",
            buildDeviceDetailStartArgs(emptyList(), partMode = null, channelIndex = 1, passageDevice = false),
        )
    }

    @Test
    fun detailEmptyWhenNoSelection() {
        assertEquals(
            "",
            buildDeviceDetailStartArgs(emptyList(), partMode = null, channelIndex = -1, passageDevice = false),
        )
    }

    @Test
    fun washingFallbackWithoutModeSendsMinusOne() {
        assertEquals(
            """{"mode":-1,"ext":[]}""",
            buildDeviceDetailStartArgs(
                emptyList(),
                partMode = null,
                channelIndex = -1,
                passageDevice = false,
                washingFallbackMode = true,
            ),
        )
    }

    @Test
    fun washingFallbackKeepsSelectedModeWithGoods() {
        val goods = listOf(DeviceGoods(pos = 1, out = 2))
        assertEquals(
            """{"mode":5,"ext":[{"pos":1,"out":2}]}""",
            buildDeviceDetailStartArgs(
                goods,
                partMode = 5,
                channelIndex = -1,
                passageDevice = false,
                washingFallbackMode = true,
            ),
        )
    }
}
