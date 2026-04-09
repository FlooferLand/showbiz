package com.flooferland.showbiz.network.packets

import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class ModelPartNamesPacket(val parent: BlockPos, val names: Map<Int, String>) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ModelPartNamesPacket>(rl("modelpart_names"))
        val codec = StreamCodec.of<FriendlyByteBuf, ModelPartNamesPacket>(
            { buf, packet ->
                buf.writeBlockPos(packet.parent)
                buf.writeVarInt(packet.names.size)
                packet.names.forEach { (key, name) ->
                    buf.writeInt(key)
                    buf.writeUtf(name)
                }
            },
            { buf ->
                val parent = buf.readBlockPos()
                val names = mutableMapOf<Int, String>()
                for (i in 0 until buf.readVarInt()) {
                    val key = buf.readInt()
                    val name = buf.readUtf()
                    names[key] = name
                }
                ModelPartNamesPacket(
                    parent = parent,
                    names = names
                )
            }
        )!!
    }
}