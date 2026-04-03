package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.utils.rl

class ShowParserEditPacket(editScreen: EditScreenMenu.EditScreenBuf) : EditScreenMenu.EditScreenPacketPayload(editScreen) {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowParserEditPacket>(rl("show_parser_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowParserEditPacket>(
            { buf, conf ->
                conf.base.encode(buf)
            },
            { buf ->
                ShowParserEditPacket(EditScreenMenu.EditScreenBuf.decode(buf))
            }
        )!!
    }
}