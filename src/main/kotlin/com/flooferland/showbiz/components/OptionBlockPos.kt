package com.flooferland.showbiz.components

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import java.util.Optional

data class OptionBlockPos(var pos: Optional<BlockPos>) {
    companion object {
        val CODEC: Codec<OptionBlockPos> = RecordCodecBuilder.create { instance ->
            instance.group(BlockPos.CODEC.optionalFieldOf("pos").forGetter { it.pos }).apply(instance, ::OptionBlockPos)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, OptionBlockPos> = StreamCodec.of(
            { buf, opt ->
                buf.writeBoolean(opt.pos.isPresent)
                if (opt.pos.isPresent) {
                    BlockPos.STREAM_CODEC.encode(buf, opt.pos.get())
                }
            },
            { buf ->
                if (buf.readBoolean())
                    OptionBlockPos(Optional.ofNullable(BlockPos.STREAM_CODEC.decode(buf)))
                else
                    OptionBlockPos(Optional.empty())
            }
        )

        val EMPTY: OptionBlockPos = OptionBlockPos(Optional.empty())
    }
}