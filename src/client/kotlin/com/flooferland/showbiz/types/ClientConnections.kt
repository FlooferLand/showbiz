package com.flooferland.showbiz.types

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import com.flooferland.showbiz.network.packets.UpdateConnectionsPacket
import com.flooferland.showbiz.types.connection.ServerConnections
import com.flooferland.showbiz.types.connection.ServerConnections.Point
import com.flooferland.showbiz.types.connection.ServerConnections.CLIENT_UPDATE_INTERVAL
import com.flooferland.showbiz.types.connection.ServerConnections.MAX_VIEW_DISTANCE_SQR
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object ClientConnections {
    val entries = mutableMapOf<BlockPos, MutableList<Point>>()
    var ticksSincePing = 0

    init {
        ClientPlayNetworking.registerGlobalReceiver(UpdateConnectionsPacket.type) { packet, _ ->
            if (packet.points.isEmpty()) entries.remove(packet.pos)
            else entries[packet.pos] = packet.points.toMutableList()
            ticksSincePing = 0
        }

        ClientPlayConnectionEvents.DISCONNECT.register { listener, minecraft ->
            entries.clear()
        }

        ClientTickEvents.END_WORLD_TICK.register { level ->
            val player = Minecraft.getInstance()?.player ?: return@register
            if (!ServerConnections.canDisplayConnections(player)) return@register

            // Clearing loose connections
            ticksSincePing += 1
            if (ticksSincePing > CLIENT_UPDATE_INTERVAL - 2) {
                for (pos in entries.keys.toList()) {
                    if (!level.isLoaded(pos) || player.distanceToSqr(pos.center) > MAX_VIEW_DISTANCE_SQR)
                        entries.remove(pos)
                }
                ticksSincePing = 0
            }
        }
    }
}