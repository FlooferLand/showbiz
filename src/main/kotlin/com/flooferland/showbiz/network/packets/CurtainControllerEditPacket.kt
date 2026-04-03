package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.utils.rl

class CurtainControllerEditPacket(editScreen: EditScreenMenu.EditScreenBuf, var bitFilterOpen: MutableList<BitId>, var bitFilterClose: MutableList<BitId>) : EditScreenMenu.EditScreenPacketPayload(editScreen) {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<CurtainControllerEditPacket>(rl("curtain_controller_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, CurtainControllerEditPacket>(
            { buf, conf ->
                conf.base.encode(buf)
                buf.writeVarIntArray(conf.bitFilterOpen.map { it.toInt() }.toIntArray())
                buf.writeVarIntArray(conf.bitFilterClose.map { it.toInt() }.toIntArray())
            },
            { buf ->
                val editScreen = EditScreenMenu.EditScreenBuf.decode(buf)
                val bitFilterOpen = buf.readVarIntArray().map { it.toBitId() }.toMutableList()
                val bitFilterClose = buf.readVarIntArray().map { it.toBitId() }.toMutableList()
                CurtainControllerEditPacket(editScreen, bitFilterOpen = bitFilterOpen, bitFilterClose = bitFilterClose)
            }
        )!!
    }
}