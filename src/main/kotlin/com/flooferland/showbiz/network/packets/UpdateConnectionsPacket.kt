package com.flooferland.showbiz.network.packets

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import com.flooferland.showbiz.types.connection.ServerConnections
import com.flooferland.showbiz.types.connection.ServerConnections.Point
import com.flooferland.showbiz.utils.rl

/** Updated connection per-block */
data class UpdateConnectionsPacket(val pos: BlockPos, val points: List<ServerConnections.Point>) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<UpdateConnectionsPacket>(rl("connections_update"))
        val codec = StreamCodec.of<FriendlyByteBuf, UpdateConnectionsPacket>(
            { buf, packet ->
                buf.writeBlockPos(packet.pos)
                buf.writeCollection(packet.points, Point::encode)
            },
            { buf -> UpdateConnectionsPacket(
                pos = buf.readBlockPos(),
                points = buf.readList { Point.decode(buf) }
            )}
        )!!
    }
}