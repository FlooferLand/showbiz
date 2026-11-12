package com.flooferland.showbiz.components

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation

data class PropComponent(var id: ResourceLocation) {
    companion object {
        val CODEC: Codec<PropComponent> = RecordCodecBuilder.create { instance ->
            instance.group(ResourceLocation.CODEC.fieldOf("id").forGetter { it.id }).apply(instance, ::PropComponent)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PropComponent> = StreamCodec.of(
            { buf, opt ->
                ResourceLocation.STREAM_CODEC.encode(buf, opt.id)
            },
            { buf ->
                PropComponent(ResourceLocation.STREAM_CODEC.decode(buf))
            }
        )
    }
}