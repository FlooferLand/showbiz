package com.flooferland.showbiz.components

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.util.*
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class FloodlightComponent(var color: Int = CommonColors.WHITE) {
    companion object {
        val CODEC: Codec<FloodlightComponent> = RecordCodecBuilder.create { instance ->
            instance.group(ExtraCodecs.ARGB_COLOR_CODEC.fieldOf("color").forGetter { it.color }).apply(instance, ::FloodlightComponent)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FloodlightComponent> = StreamCodec.of(
            { buf, opt ->
                ByteBufCodecs.INT.encode(buf, opt.color)
            },
            { buf -> FloodlightComponent(
                color = ByteBufCodecs.INT.decode(buf)
            ) }
        )
    }
}