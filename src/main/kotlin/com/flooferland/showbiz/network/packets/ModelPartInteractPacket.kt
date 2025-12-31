package com.flooferland.showbiz.network.packets

import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl
import java.util.UUID

class ModelPartInteractPacket(val name: String, val parent: BlockPos, val player: UUID) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ModelPartInteractPacket>(rl("modelpart_interact"))
        val codec = StreamCodec.of<FriendlyByteBuf, ModelPartInteractPacket>(
            { buf, packet ->
                buf.writeUtf(packet.name)
                buf.writeBlockPos(packet.parent)
                buf.writeUUID(packet.player)
            },
            { buf ->
                val name = buf.readUtf()
                val parent = buf.readBlockPos()
                val player = buf.readUUID()
                ModelPartInteractPacket(
                    name = name,
                    parent = parent,
                    player = player
                )
            }
        )!!
    }
}