package com.flooferland.showbiz.registry

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.network.packets.ModelPartInteractPacket
import com.flooferland.showbiz.network.packets.PlaybackChunkPacket
import com.flooferland.showbiz.network.packets.PlaybackStatePacket
import com.flooferland.showbiz.network.packets.ShowParserDataPacket
import com.flooferland.showbiz.registry.ModPackets.PacketRegistryWay.*
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

sealed class ModPackets<T: CustomPacketPayload> {
    data object PlaybackChunk : ModPackets<PlaybackChunkPacket>(ServerToClient, PlaybackChunkPacket.type, PlaybackChunkPacket.codec)
    data object PlaybackState : ModPackets<PlaybackStatePacket>(ServerToClient, PlaybackStatePacket.type, PlaybackStatePacket.codec)
    data object ShowParserData : ModPackets<ShowParserDataPacket>(Bidirectional, ShowParserDataPacket.type, ShowParserDataPacket.codec)
    data object ModelPartInteract : ModPackets<ModelPartInteractPacket>(ClientToServer, ModelPartInteractPacket.type, ModelPartInteractPacket.codec)

    constructor(way: PacketRegistryWay, type: CustomPacketPayload.Type<T>, codec: StreamCodec<FriendlyByteBuf, T>) {
        when (way) {
            ServerToClient -> PayloadTypeRegistry.playS2C().register(type, codec)
            ClientToServer -> PayloadTypeRegistry.playC2S().register(type, codec)
            Bidirectional -> {
                PayloadTypeRegistry.playS2C().register(type, codec)
                PayloadTypeRegistry.playC2S().register(type, codec)
            }
        }
    }

    enum class PacketRegistryWay { /** S2C */ ServerToClient, /** C2S */ ClientToServer, Bidirectional }

    companion object {
        init { ModPackets::class.sealedSubclasses.forEach { it.objectInstance } }
    }
}