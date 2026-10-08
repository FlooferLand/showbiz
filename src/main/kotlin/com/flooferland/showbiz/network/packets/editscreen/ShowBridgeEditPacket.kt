package com.flooferland.showbiz.network.packets.editscreen

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.utils.rl

class ShowBridgeEditPacket(editScreen: EditScreenMenu.EditScreenBuf) : EditScreenMenu.EditScreenPacketPayload(editScreen) {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowBridgeEditPacket>(rl("show_bridge_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowBridgeEditPacket>(
            { buf, conf ->
                conf.base.encode(buf)
            },
            { buf ->
                ShowBridgeEditPacket(EditScreenMenu.EditScreenBuf.decode(buf))
            }
        )!!
    }
}