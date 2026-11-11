package com.flooferland.showbiz

import com.flooferland.showbiz.Permissions.Companion.mayInteractWith
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.network.packets.ClientCapabilitiesPacket
import com.flooferland.showbiz.network.packets.ModelPartInteractPacket
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.SemVer
import com.flooferland.showbiz.types.modelpart.IModelPartInteractable
import com.flooferland.showbiz.types.modelpart.ModelPartManager
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import com.flooferland.showbiz.utils.Extensions.notifyPermissionError
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
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

            val blockEntity = level.getBlockEntity(packet.parent) ?: return@listen
            if (!player.mayInteractWith(blockEntity)) {
                player.notifyPermissionError()
                return@listen
            }

            val interactable = blockEntity as? IModelPartInteractable ?: return@listen
            val key = interactable.getInteractionMapping()[packet.name] ?: return@listen
            interactable.onInteract(key, level, player)
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

        ServerPackets.listen(ClientCapabilitiesPacket.type) { packet, ctx ->
            val player = ctx.player() ?: return@listen
            val playerAddons = packet.addons
            for ((addonId, addon) in Showbiz.addons) {
                // Checking presence
                if (!playerAddons.containsKey(addonId)) {
                    val reason = Component.literal("Missing Showbiz addon ").append(Component.literal("$addonId v${addon.manifest.version}").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("\n\n").withStyle(ChatFormatting.RESET))
                        .append(Component.literal("Installed addons:\n").append("[${playerAddons.keys.sorted().joinToString(", ")}]\n\n"))
                        .append(Component.literal("Server addons:\n").append("[${Showbiz.addons.keys.sorted().joinToString(", ")}]\n\n"))
                    player.connection.send(ClientboundDisconnectPacket(reason))
                    return@listen
                }

                // Getting the versions (skips if null cuz the manifest might be missing on the client)
                val serverVersion = SemVer(addon.manifest.version)
                val playerVersion = playerAddons[addonId] ?: run {
                    Showbiz.log.warn("Couldn't find a version for the addon '$addonId'")
                    continue
                }

                // Comparing
                if (playerVersion.lesserThan(serverVersion) ?: false) {
                    val reason = Component.literal("Showbiz addon out of date: ")
                        .append(Component.literal(addonId).withStyle(ChatFormatting.GRAY))
                        .append(" (v${playerVersion})\n")
                        .append("v$serverVersion is installed on the server")
                    player.connection.send(ClientboundDisconnectPacket(reason))
                    return@listen
                }
                if (playerVersion.string != serverVersion.string) {
                    player.sendSystemMessage(Component.literal("Showbiz addon '${addonId}' version mismatch ($playerVersion != $serverVersion)"))
                }
            }

            // Saving the capabilities
            Showbiz.playerPrefs[player.uuid] = packet
            Showbiz.log.debug("Player '${player.name.string}' preferences: ${packet.toString()}")
        }
    }
}