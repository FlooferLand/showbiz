package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.connection.ConnectionOwnerId
import com.flooferland.showbiz.utils.rl

class ConnectionDataPacket(val id: ConnectionOwnerId, val portId: String, val data: ByteArray) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ConnectionDataPacket>(rl("cdata"))
        val codec = StreamCodec.of<FriendlyByteBuf, ConnectionDataPacket>(
            { buf, packet ->
                packet.id.encode(buf)
                buf.writeUtf(packet.portId, 16)
                buf.writeByteArray(packet.data)
            },
            { buf ->
                val id = ConnectionOwnerId.decode(buf)
                val portId = buf.readUtf(16)
                val data = buf.readByteArray()
                ConnectionDataPacket(id, portId, data)
            }
        )!!
    }
}