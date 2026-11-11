package com.flooferland.showbiz

import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.network.packets.ClientCapabilitiesPacket
import com.flooferland.showbiz.network.packets.OpenClientConfigPacket
import com.flooferland.showbiz.network.packets.ServerCapabilitiesPacket
import com.flooferland.showbiz.network.packets.ShowPlaybackStatePacket
import com.flooferland.showbiz.screens.ShowbizConfigScreen
import com.flooferland.showbiz.types.FFmpeg
import com.flooferland.showbiz.types.SemVer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

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

    fun sendCapabilities() {
        val addons = ShowbizClient.addons.mapValues { (_, v) -> v.manifest?.version?.let { SemVer(it) } }
        if (ClientPlayNetworking.canSend(ClientCapabilitiesPacket.type)) {
            ClientPlayNetworking.send(ClientCapabilitiesPacket(Showbiz.config, addons))
        }
    }

    fun init() {
        // Server capabilities
        listen(ServerCapabilitiesPacket.type) { packet, ctx ->
            FFmpeg.serverAvailable = packet.hasFFmpeg
            Showbiz.log.debug("Server capabilities: {}", packet.toString())
        }

        // Show playback
        listen(ShowPlaybackStatePacket.type) { packet, context ->
            val level = context.player().level() as? ClientLevel ?: return@listen
            val blockEntity = level.getBlockEntity(packet.blockPos) as? ReelToReelBlockEntity ?: return@listen
            blockEntity.clientApplyPlaybackState(packet)
        }

        // Opening the config screen
        listen(OpenClientConfigPacket.type) { _, context ->
            val client = context.client() ?: return@listen
            client.setScreen(ShowbizConfigScreen(client.screen))
        }
    }
}