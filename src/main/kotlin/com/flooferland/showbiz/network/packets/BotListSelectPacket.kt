package com.flooferland.showbiz.network.packets

import net.minecraft.core.BlockPos
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.utils.rl

class BotListSelectPacket(val blockPos: BlockPos, val id: ResourceId?) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<BotListSelectPacket>(rl("bot_list_select"))
        val codec = StreamCodec.of<FriendlyByteBuf, BotListSelectPacket>(
            { buf, packet ->
                buf.writeBlockPos(packet.blockPos)
                buf.writeNullable(packet.id, ResourceId::encode)
            },
            { buf ->
                BotListSelectPacket(
                    blockPos = buf.readBlockPos(),
                    id = buf.readNullable { ResourceId.decode(it) }
                )
            }
        )!!
    }
}