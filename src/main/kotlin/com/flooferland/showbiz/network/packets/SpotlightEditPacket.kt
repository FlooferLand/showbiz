package com.flooferland.showbiz.network.packets

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.math.Vec2f
import com.flooferland.showbiz.types.math.Vec3fc
import com.flooferland.showbiz.utils.rl

class SpotlightEditPacket(val blockPos: BlockPos, val bitFilter: MutableList<BitId>, var turn: Vec2f, val mapping: String?) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<SpotlightEditPacket>(rl("spotlight_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, SpotlightEditPacket>(
            { buf, conf ->
                buf.writeBlockPos(conf.blockPos)
                buf.writeVarIntArray(conf.bitFilter.map { it.toInt() }.toIntArray())
                buf.writeFloat(conf.turn.x)
                buf.writeFloat(conf.turn.y)
                buf.writeUtf(conf.mapping ?: "")
            },
            { buf ->
                val blockPos = buf.readBlockPos()
                val bitFilter = buf.readVarIntArray().map { it.toBitId() }.toMutableList()
                val turnX = buf.readFloat()
                val turnY = buf.readFloat()
                val mapping = buf.readUtf()
                SpotlightEditPacket(blockPos = blockPos, bitFilter = bitFilter, turn = Vec2f(turnX, turnY), mapping = mapping)
            }
        )!!
    }
}