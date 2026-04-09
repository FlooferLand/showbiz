package com.flooferland.showbiz.registry

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.network.packets.*
import com.flooferland.showbiz.registry.ModPackets.PacketRegistryWay.*
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

sealed class ModPackets<T: CustomPacketPayload> {
    data object PlaybackChunk : ModPackets<PlaybackChunkPacket>(ServerToClient, PlaybackChunkPacket.type, PlaybackChunkPacket.codec)
    data object PlaybackState : ModPackets<PlaybackStatePacket>(ServerToClient, PlaybackStatePacket.type, PlaybackStatePacket.codec)
    data object ShowParserData : ModPackets<ShowParserEditPacket>(Bidirectional, ShowParserEditPacket.type, ShowParserEditPacket.codec)
    data object SpotlightEdit : ModPackets<SpotlightEditPacket>(Bidirectional, SpotlightEditPacket.type, SpotlightEditPacket.codec)
    data object ModelPartInteract : ModPackets<ModelPartInteractPacket>(ClientToServer, ModelPartInteractPacket.type, ModelPartInteractPacket.codec)
    data object ModelPartNames : ModPackets<ModelPartNamesPacket>(ServerToClient, ModelPartNamesPacket.type, ModelPartNamesPacket.codec)
    data object ShowFileList : ModPackets<ShowFileListPacket>(Bidirectional, ShowFileListPacket.type, ShowFileListPacket.codec)
    data object ShowFileSelect : ModPackets<ShowFileSelectPacket>(ClientToServer, ShowFileSelectPacket.type, ShowFileSelectPacket.codec)
    data object BotList : ModPackets<BotListPacket>(Bidirectional, BotListPacket.type, BotListPacket.codec)
    data object ShowListSelect : ModPackets<BotListSelectPacket>(ClientToServer, BotListSelectPacket.type, BotListSelectPacket.codec)
    data object CurtainControllerEdit: ModPackets<CurtainControllerEditPacket>(ClientToServer, CurtainControllerEditPacket.type, CurtainControllerEditPacket.codec)
    data object UpdateConnections: ModPackets<UpdateConnectionsPacket>(ServerToClient, UpdateConnectionsPacket.type, UpdateConnectionsPacket.codec)

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