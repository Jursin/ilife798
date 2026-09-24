package com.github.ilife798.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class BillNamesTest {
    @Test
    fun productTypeNameMapping() {
        assertEquals("钱包充值", productTypeName(1))
        assertEquals("设备消费", productTypeName(6))
        assertEquals("VIP会员卡", productTypeName(9))
        assertEquals("无限制", productTypeName(-1))
        assertEquals("未知类型", productTypeName(999))
    }

    @Test
    fun paymentTypeNameMapping() {
        assertEquals("商家钱包", paymentTypeName(91))
        assertEquals("支付宝（APP）", paymentTypeName(21))
        assertEquals("微信（小程序）", paymentTypeName(13))
        assertEquals("其他", paymentTypeName(999))
    }

    @Test
    fun billStatusNameMapping() {
        assertEquals("已付款", billStatusName(3, dir = 1))
        assertEquals("未付款", billStatusName(1, dir = 1))
        assertEquals("付款失败", billStatusName(4, dir = 1))
        assertEquals("未知状态", billStatusName(99, dir = 1))
        // dir=2 为退款方向
        assertEquals("已退款", billStatusName(3, dir = 2))
        assertEquals("退款中", billStatusName(1, dir = 2))
    }
}
