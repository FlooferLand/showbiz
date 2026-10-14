package com.flooferland.showbiz.types.math

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

/** Mutable float Vector2 */
data class Vec2f(var x: Float = 0f, var y: Float = 0f) {
    companion object {
        val ZERO get() = Vec2f()
        val CODEC: Codec<Vec2f> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.FLOAT.fieldOf("x").forGetter { it.x },
                Codec.FLOAT.fieldOf("y").forGetter { it.y }
            ).apply(instance, ::Vec2f)
        }
    }
}