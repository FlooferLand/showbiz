package com.flooferland.showbiz.network.packets

import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.utils.rl
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class BotListSelectPacket(val bot: OwnerId, val botId: ResourceId?, val botVariant: String) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<BotListSelectPacket>(rl("bot_list_select"))
        val codec = StreamCodec.of<FriendlyByteBuf, BotListSelectPacket>(
            { buf, packet ->
                packet.bot.encode(buf)
                buf.writeNullable(packet.botId, ResourceId::encode)
                buf.writeUtf(packet.botVariant)
            },
            { buf ->
                BotListSelectPacket(
                    bot = OwnerId.decode(buf),
                    botId = buf.readNullable { ResourceId.decode(it) },
                    botVariant = buf.readUtf()
                )
            }
        )!!
    }
}