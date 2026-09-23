package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.math.Vec2f
import com.flooferland.showbiz.utils.rl

class FloodlightEditPacket(editScreen: EditScreenMenu.EditScreenBuf, var turn: Vec2f, var angle: Float, var shadows: Boolean, var color: Int, var brightness: Float) : EditScreenMenu.EditScreenPacketPayload(editScreen) {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<FloodlightEditPacket>(rl("floodlight_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, FloodlightEditPacket>(
            { buf, conf ->
                conf.base.encode(buf)
                buf.writeFloat(conf.turn.x)
                buf.writeFloat(conf.turn.y)
                buf.writeFloat(conf.angle)
                buf.writeBoolean(conf.shadows)
                buf.writeInt(conf.color)
                buf.writeFloat(conf.brightness)
            },
            { buf ->
                val editScreen = EditScreenMenu.EditScreenBuf.decode(buf)
                val turnX = buf.readFloat()
                val turnY = buf.readFloat()
                val angle = buf.readFloat()
                val shadows = buf.readBoolean()
                val color = buf.readInt()
                val brightness = buf.readFloat()
                FloodlightEditPacket(editScreen, turn = Vec2f(turnX, turnY), angle = angle, shadows = shadows, color = color, brightness = brightness)
            }
        )!!
    }
}