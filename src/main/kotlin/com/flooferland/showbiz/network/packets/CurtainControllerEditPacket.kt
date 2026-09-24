package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.rl

class CurtainControllerEditPacket(editScreen: EditScreenMenu.EditScreenBuf, var bitFilterOpen: MappedBits, var bitFilterClose: MappedBits) : EditScreenMenu.EditScreenPacketPayload(editScreen) {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<CurtainControllerEditPacket>(rl("curtain_controller_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, CurtainControllerEditPacket>(
            { buf, conf ->
                conf.base.encode(buf)
                ByteBufCodecs.fromCodec(MappedBits.CODEC).encode(buf, conf.bitFilterOpen)
                ByteBufCodecs.fromCodec(MappedBits.CODEC).encode(buf, conf.bitFilterClose)
            },
            { buf ->
                val editScreen = EditScreenMenu.EditScreenBuf.decode(buf)
                val bitFilterOpen = ByteBufCodecs.fromCodec(MappedBits.CODEC).decode(buf)
                val bitFilterClose = ByteBufCodecs.fromCodec(MappedBits.CODEC).decode(buf)
                CurtainControllerEditPacket(editScreen, bitFilterOpen = bitFilterOpen, bitFilterClose = bitFilterClose)
            }
        )!!
    }
}