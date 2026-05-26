package com.flooferland.showbiz

import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.network.packets.ModelPartInteractPacket
import com.flooferland.showbiz.types.modelpart.IModelPartInteractable
import com.flooferland.showbiz.types.modelpart.ModelPartManager
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

/** Prevents Fabric's broken as hell packet system from silently not registering receivers on the same packet */
object ServerPackets {
    private val handlers = hashMapOf<CustomPacketPayload.Type<*>, MutableList<ServerPlayNetworking.PlayPayloadHandler<*>>>()
    private val registered = hashSetOf<CustomPacketPayload.Type<*>>()

    /** Can register and run several handler for the same packet. Ensures the block runs on the server thread */
    @Suppress("UNCHECKED_CAST")
    fun <T : CustomPacketPayload> listen(type: CustomPacketPayload.Type<T>, handler: ServerPlayNetworking.PlayPayloadHandler<T>) {
        ServerPackets.handlers.getOrPut(type) { mutableListOf() }.add(handler)
        if (!registered.add(type)) return
        ServerPlayNetworking.registerGlobalReceiver(type) { packet, context ->
            context.server().execute {
                ServerPackets.handlers[packet.type()]?.forEach {
                    (it as ServerPlayNetworking.PlayPayloadHandler<CustomPacketPayload>).receive(packet, context)
                }
            }
        }
    }

    fun init() {
        // Model parts
        ServerPackets.listen(ModelPartInteractPacket.type) { packet, context ->
            val player = context.player()
            if (player.uuid != packet.player) return@listen
            val level = player.serverLevel() ?: return@listen

            // TODO: Do a more secure distance check for model part interaction (raycast)
            if (player.position().distanceTo(packet.parent.center) > ModelPartManager.getMaxReach(player) + 1f) {
                Showbiz.log.info("Player ${player.name} shouldn't have been able to reach a modelpart block. Reach hacks?")
                return@listen
            }

            val blockEntity = level.getBlockEntity(packet.parent) as? IModelPartInteractable ?: return@listen
            val key = blockEntity.getInteractionMapping()[packet.name] ?: return@listen
            blockEntity.onInteract(key, level, player)
        }
    }
}