package com.flooferland.showbiz.network.packets.editscreen

import com.flooferland.showbiz.types.CurtainState
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.rl
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class CurtainControllerEditPacket(editScreen: EditScreenMenu.EditScreenBuf, var bitFilterOpen: MappedBits, var bitFilterClose: MappedBits, var defaultState: CurtainState, var speed: Float) : EditScreenMenu.EditScreenPacketPayload(editScreen) {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<CurtainControllerEditPacket>(rl("curtain_controller_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, CurtainControllerEditPacket>(
            { buf, conf ->
                conf.base.encode(buf)
                MappedBits.STREAM_CODEC.encode(buf, conf.bitFilterOpen)
                MappedBits.STREAM_CODEC.encode(buf, conf.bitFilterClose)
                buf.writeEnum(conf.defaultState)
                buf.writeFloat(conf.speed)
            },
            { buf ->
                val editScreen = EditScreenMenu.EditScreenBuf.decode(buf)
                val bitFilterOpen = MappedBits.STREAM_CODEC.decode(buf)
                val bitFilterClose = MappedBits.STREAM_CODEC.decode(buf)
                val defaultState = buf.readEnum(CurtainState::class.java)
                val speed = buf.readFloat()
                CurtainControllerEditPacket(editScreen, bitFilterOpen = bitFilterOpen, bitFilterClose = bitFilterClose, defaultState = defaultState, speed = speed)
            }
        )!!
    }
}