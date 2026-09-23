package com.github.ilife798.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.github.ilife798.shared.resources.Res
import com.github.ilife798.shared.resources.device_type_access_control
import com.github.ilife798.shared.resources.device_type_card_recharge
import com.github.ilife798.shared.resources.device_type_charging_pile
import com.github.ilife798.shared.resources.device_type_direct_water
import com.github.ilife798.shared.resources.device_type_drinking
import com.github.ilife798.shared.resources.device_type_dryer
import com.github.ilife798.shared.resources.device_type_hair_drying
import com.github.ilife798.shared.resources.device_type_laundry
import com.github.ilife798.shared.resources.device_type_meter
import com.github.ilife798.shared.resources.device_type_other
import com.github.ilife798.shared.resources.device_type_pos
import com.github.ilife798.shared.resources.device_type_power_bank
import com.github.ilife798.shared.resources.device_type_shoes
import com.github.ilife798.shared.resources.device_type_shower
import com.github.ilife798.shared.resources.device_type_shower_gel
import com.github.ilife798.shared.resources.device_type_telephone
import com.github.ilife798.shared.resources.device_type_vending
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// 图标圆底蓝
private val DeviceIconBackground = Color(0xFF40BBFD)

// 白色剪影前景（p.f 映射 ic_dev_{dtype}），无专属图的 dtype 落通用图
fun deviceIconRes(dtype: Int): DrawableResource =
    when (dtype) {
        5 -> Res.drawable.device_type_direct_water
        6 -> Res.drawable.device_type_shower
        8 -> Res.drawable.device_type_drinking
        10 -> Res.drawable.device_type_laundry
        20 -> Res.drawable.device_type_hair_drying
        21 -> Res.drawable.device_type_shower_gel
        30 -> Res.drawable.device_type_vending
        35 -> Res.drawable.device_type_card_recharge
        45 -> Res.drawable.device_type_telephone
        50 -> Res.drawable.device_type_access_control
        60 -> Res.drawable.device_type_pos
        75 -> Res.drawable.device_type_power_bank
        80 -> Res.drawable.device_type_dryer
        90 -> Res.drawable.device_type_shoes
        95 -> Res.drawable.device_type_meter
        120 -> Res.drawable.device_type_charging_pile
        else -> Res.drawable.device_type_other
    }

// 圆形蓝底 + 白色前景
@Composable
fun DeviceIcon(
    dtype: Int,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(DeviceIconBackground),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(deviceIconRes(dtype)),
            contentDescription = null,
            modifier = Modifier.size(size * 1.4f),
            contentScale = ContentScale.Fit,
        )
    }
}
