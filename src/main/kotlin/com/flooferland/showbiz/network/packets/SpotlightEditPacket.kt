package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.math.Vec2f
import com.flooferland.showbiz.utils.rl

class SpotlightEditPacket(editScreen: EditScreenMenu.EditScreenBuf, var turn: Vec2f = Vec2f.ZERO) : EditScreenMenu.EditScreenPacketPayload(editScreen) {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<SpotlightEditPacket>(rl("spotlight_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, SpotlightEditPacket>(
            { buf, conf ->
                conf.base.encode(buf)
                buf.writeFloat(conf.turn.x)
                buf.writeFloat(conf.turn.y)
            },
            { buf ->
                val editScreen = EditScreenMenu.EditScreenBuf.decode(buf)
                val turnX = buf.readFloat()
                val turnY = buf.readFloat()
                SpotlightEditPacket(editScreen, turn = Vec2f(turnX, turnY))
            }
        )!!
    }
}