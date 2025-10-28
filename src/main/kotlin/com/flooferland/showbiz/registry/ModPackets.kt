package com.flooferland.showbiz.registry

import com.flooferland.showbiz.network.base.PlaybackChunkPacket
import com.flooferland.showbiz.network.base.PlaybackStatePacket
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

object ModPackets {
    fun registerS2C() {
        PayloadTypeRegistry.playS2C().register(PlaybackChunkPacket.type, PlaybackChunkPacket.codec)
        PayloadTypeRegistry.playS2C().register(PlaybackStatePacket.type, PlaybackStatePacket.codec)
    }

    @Environment(EnvType.CLIENT)
    fun registerC2S() {

    }
}