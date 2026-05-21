package com.flooferland.showbiz.components

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.resources.ResourceLocation

data class PlushComponent(var id: ResourceLocation) {
    companion object {
        val CODEC: Codec<PlushComponent> = RecordCodecBuilder.create { instance ->
            instance.group(ResourceLocation.CODEC.fieldOf("id").forGetter { it.id }).apply(instance, ::PlushComponent)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PlushComponent> = StreamCodec.of(
            { buf, opt ->
                ResourceLocation.STREAM_CODEC.encode(buf, opt.id)
            },
            { buf ->
                PlushComponent(ResourceLocation.STREAM_CODEC.decode(buf))
            }
        )
    }
}