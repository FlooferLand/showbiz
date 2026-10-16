package com.flooferland.showbiz.network.packets.editscreen

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.blocks.entities.ShowParserBlockEntity
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.rl

class ShowParserEditPacket(editScreen: EditScreenMenu.EditScreenBuf, val bitFilter: MappedBits, var mode: ShowParserBlockEntity.Mode, var invert: Boolean) : EditScreenMenu.EditScreenPacketPayload(editScreen) {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowParserEditPacket>(rl("show_parser_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowParserEditPacket>(
            { buf, conf ->
                conf.base.encode(buf)
                MappedBits.STREAM_CODEC.encode(buf, conf.bitFilter)
                buf.writeEnum(conf.mode)
                buf.writeBoolean(conf.invert)
            },
            { buf ->
                ShowParserEditPacket(
                    EditScreenMenu.EditScreenBuf.decode(buf),
                    bitFilter = MappedBits.STREAM_CODEC.decode(buf),
                    mode = buf.readEnum(ShowParserBlockEntity.Mode::class.java),
                    invert = buf.readBoolean()
                )
            }
        )!!
    }
}