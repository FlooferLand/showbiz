package com.flooferland.showbiz

import net.minecraft.client.multiplayer.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.network.packets.PlaybackStatePacket
import com.flooferland.showbiz.network.packets.ServerCapabilitiesPacket
import com.flooferland.showbiz.types.FFmpeg
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

/** Prevents Fabric's broken as hell packet system from silently not registering receivers on the same packet */
object ClientPackets {
    private val handlers = hashMapOf<CustomPacketPayload.Type<*>, MutableList<ClientPlayNetworking.PlayPayloadHandler<*>>>()
    private val registered = hashSetOf<CustomPacketPayload.Type<*>>()

    /** Can register and run several handler for the same packet. Ensures the block runs on the client thread */
    @Suppress("UNCHECKED_CAST")
    fun <T : CustomPacketPayload> listen(type: CustomPacketPayload.Type<T>, handler: ClientPlayNetworking.PlayPayloadHandler<T>) {
        handlers.getOrPut(type) { mutableListOf() }.add(handler)
        if (!registered.add(type)) return
        ClientPlayNetworking.registerGlobalReceiver(type) { packet, context ->
            context.client().execute {
                handlers[packet.type()]?.forEach {
                    (it as ClientPlayNetworking.PlayPayloadHandler<CustomPacketPayload>).receive(packet, context)
                }
            }
        }
    }

    fun init() {
        // Server capabilities
        listen(ServerCapabilitiesPacket.type) { packet, ctx ->
            FFmpeg.serverAvailable = packet.hasFFmpeg
            Showbiz.log.debug("Server capabilities: {}", packet.toString())
        }

        // Show playback
        listen(PlaybackStatePacket.type) { packet, context ->
            val level = context.player().level() as? ClientLevel ?: return@listen
            val blockEntity = level.getBlockEntity(packet.blockPos) as? ReelToReelBlockEntity ?: return@listen
            blockEntity.clientApplyPlaybackState(packet)
        }
    }
}