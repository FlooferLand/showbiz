package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.connection.ConnectionOwnerId
import com.flooferland.showbiz.types.connection.ServerConnections
import com.flooferland.showbiz.types.connection.ServerConnections.Point
import com.flooferland.showbiz.utils.rl

/** Updated connection per-block */
data class UpdateConnectionsPacket(val id: ConnectionOwnerId, val points: List<ServerConnections.Point>) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<UpdateConnectionsPacket>(rl("connections_update"))
        val codec = StreamCodec.of<FriendlyByteBuf, UpdateConnectionsPacket>(
            { buf, packet ->
                packet.id.encode(buf)
                buf.writeCollection(packet.points, Point::encode)
            },
            { buf ->
                val id = ConnectionOwnerId.decode(buf)
                val points = buf.readList { Point.decode(buf) }
                UpdateConnectionsPacket(id, points)
            }
        )!!
    }
}