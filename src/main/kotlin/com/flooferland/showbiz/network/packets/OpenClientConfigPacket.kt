package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class OpenClientConfigPacket() : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<OpenClientConfigPacket>(rl("open_client_config"))
        val codec = StreamCodec.of<FriendlyByteBuf, OpenClientConfigPacket>(
            { _, _ -> },
            { _ -> OpenClientConfigPacket() }
        )!!
    }
}