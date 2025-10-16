package com.flooferland.showbiz.network.base

import net.minecraft.network.protocol.common.custom.*

class PlaybackPacket : CustomPacketPayload {
    // val type = CustomPacketPayload.Type()

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?>? {
        TODO("Not yet implemented")
    }
}