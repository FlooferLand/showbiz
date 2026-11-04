package com.flooferland.showbiz

import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.network.packets.ModelPartInteractPacket
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.modelpart.IModelPartInteractable
import com.flooferland.showbiz.types.modelpart.ModelPartManager
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.entity.BlockEntity

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

        ServerPackets.listen(BotListSelectPacket.type) { packet, ctx ->
            val bot = packet.bot.grabConnectable(ctx.player().level()) as? IBot ?: return@listen
            fun hash() = bot.botId.hashCode() + bot.botVariant.hashCode()

            val before = hash()
            bot.botId = packet.botId
            bot.botVariant = packet.botVariant
            if (bot is BlockEntity) bot.markDirtyNotifyAll()

            if (before != hash()) {
                ctx.player().playNotifySound(ModSounds.Select.event, SoundSource.PLAYERS, 0.1f, 1.4f)
            }
        }
    }
}