package com.flooferland.showbiz.types

import net.minecraft.client.*
import net.minecraft.network.*
import com.flooferland.showbiz.ClientPackets
import com.flooferland.showbiz.network.packets.ConnectionDataPacket
import com.flooferland.showbiz.network.packets.UpdateConnectionsPacket
import com.flooferland.showbiz.types.connection.ConnectionOwnerId
import com.flooferland.showbiz.types.connection.ServerConnections
import com.flooferland.showbiz.types.connection.ServerConnections.CLIENT_UPDATE_INTERVAL
import com.flooferland.showbiz.types.connection.ServerConnections.MAX_VIEW_DISTANCE_SQR
import com.flooferland.showbiz.types.connection.ServerConnections.Point
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

object ClientConnections {
    val entries = mutableMapOf<ConnectionOwnerId, MutableList<Point>>()
    var ticksSincePing = 0

    init {
        ClientPackets.listen(UpdateConnectionsPacket.type) { packet, _ ->
            if (packet.points.isEmpty()) entries.remove(packet.id)
            else entries[packet.id] = packet.points.toMutableList()
            ticksSincePing = 0
        }

        ClientPackets.listen(ConnectionDataPacket.type) { packet, ctx ->
            val level = Minecraft.getInstance().level ?: return@listen
            val connectable = packet.id.grabConnectable(level) ?: return@listen
            val port = connectable.connectionManager.inputs[packet.portId] ?: return@listen
            val buf = FriendlyByteBuf(Unpooled.wrappedBuffer(packet.data))
            port.data.decode(buf)
            buf.release()
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
                for (ownerId in entries.keys.toList()) {
                    if (!ownerId.isLoaded(level)) continue
                    val pos = ownerId.grabPos(level) ?: continue
                    if (player.distanceToSqr(pos) > MAX_VIEW_DISTANCE_SQR)
                        entries.remove(ownerId)
                }
                ticksSincePing = 0
            }
        }
    }
}