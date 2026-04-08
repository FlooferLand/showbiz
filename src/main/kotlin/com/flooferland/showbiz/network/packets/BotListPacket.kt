package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.utils.rl

class BotListPacket(val toClient: Boolean = false, val bots: Map<ResourceId, AddonBotEntry> = mapOf()) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<BotListPacket>(rl("bot_list"))
        val codec = StreamCodec.of<FriendlyByteBuf, BotListPacket>(
            { buf, packet ->
                buf.writeBoolean(packet.toClient)
                if (packet.toClient) {
                    buf.writeShort(packet.bots.size)
                    for ((id, entry) in packet.bots) {
                        ResourceId.encode(buf, id)
                        entry.encode(buf)
                    }
                }
            },
            { buf ->
                val isResponse = buf.readBoolean()
                if (isResponse) {
                    val size = buf.readShort().toInt()
                    val bots = mutableMapOf<ResourceId, AddonBotEntry>()
                    (0 until size).forEach { _ ->
                        val id = ResourceId.decode(buf)
                        val entry = AddonBotEntry.decode(buf)
                        bots[id] = entry
                    }
                    BotListPacket(true, bots)
                } else {
                    BotListPacket(false)
                }
            }
        )!!
    }
}