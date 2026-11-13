package com.flooferland.showbiz.components

import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.types.math.Vec2f
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.CommonColors
import net.minecraft.util.ExtraCodecs

data class FloodlightComponent(var turn: Vec2f = Vec2f.ZERO, var angle: Float = 45f, var shadows: Boolean = false, var color: Int = CommonColors.WHITE, var bitFilter: MappedBits = MappedBits()) {
    companion object {
        fun from(entity: FloodlightEntity) = FloodlightComponent(
            turn = entity.turn,
            angle = entity.angle,
            shadows = entity.shadows,
            color = entity.color,
            bitFilter = entity.bitFilter.clone()
        )

        val CODEC: Codec<FloodlightComponent> = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec2f.CODEC.fieldOf("turn").forGetter { it.turn },
                Codec.FLOAT.fieldOf("angle").forGetter { it.angle },
                Codec.BOOL.fieldOf("shadows").forGetter { it.shadows },
                ExtraCodecs.ARGB_COLOR_CODEC.fieldOf("color").forGetter { it.color },
                MappedBits.CODEC.fieldOf("bit_filter").forGetter { it.bitFilter }
            ).apply(instance, ::FloodlightComponent)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FloodlightComponent> =
            ByteBufCodecs.fromCodecWithRegistries(CODEC)
    }
}