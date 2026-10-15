package com.flooferland.showbiz.network.packets.editscreen

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.showbridge.ShowBridgeProfile
import com.flooferland.showbiz.utils.rl

class ShowBridgeEditPacket(editScreen: EditScreenMenu.EditScreenBuf, var serverProfile: ShowBridgeProfile, var playerProfile: ShowBridgeProfile, var serverPorts: List<String> = emptyList()) : EditScreenMenu.EditScreenPacketPayload(editScreen) {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowBridgeEditPacket>(rl("show_bridge_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowBridgeEditPacket>(
            { buf, conf ->
                conf.base.encode(buf)
                ShowBridgeProfile.codec.encode(buf, conf.serverProfile)
                ShowBridgeProfile.codec.encode(buf, conf.playerProfile)
                buf.writeCollection(conf.serverPorts) { buf, string -> buf.writeUtf(string) }
            },
            { buf ->
                ShowBridgeEditPacket(
                    editScreen = EditScreenMenu.EditScreenBuf.decode(buf),
                    serverProfile = ShowBridgeProfile.codec.decode(buf),
                    playerProfile = ShowBridgeProfile.codec.decode(buf),
                    serverPorts = buf.readList { it.readUtf() }
                )
            }
        )!!
    }
}