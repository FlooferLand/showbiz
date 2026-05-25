package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

data class ServerCapabilitiesPacket(val hasFFmpeg: Boolean) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ServerCapabilitiesPacket>(rl("server_capabilities"))
        val codec = StreamCodec.of<FriendlyByteBuf, ServerCapabilitiesPacket>(
            { buf, packet ->
                buf.writeBoolean(packet.hasFFmpeg)
            },
            { buf ->
                ServerCapabilitiesPacket(hasFFmpeg = buf.readBoolean())
            }
        )!!
    }
}