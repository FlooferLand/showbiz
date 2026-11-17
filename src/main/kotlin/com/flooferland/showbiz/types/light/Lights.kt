package com.flooferland.showbiz.types.light

import com.flooferland.showbiz.types.math.Color4
import net.minecraft.world.level.lighting.LightEngine
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.math.min
import kotlin.math.roundToInt

sealed class AbstractLight {
    var enabled: Boolean = true
    var color: Color4 = Color4.WHITE
    var intensity: Float = 1f
    var shadows: Boolean = true

    var localPos: Vec3 = Vec3(0.0, 0.0, 0.0)

    fun getLightLevel(): Int {
        val color = color.toLinear()
        val value = min(color.a.toDouble(), (color.luminance * 10.0)) * intensity.toDouble()
        return (value * LightEngine.MAX_LEVEL).roundToInt().coerceIn(0, LightEngine.MAX_LEVEL)
    }
}

data class PointLight(
    var radius: Float = 1f,
) : AbstractLight()

data class SpotLight(
    var dir: Vec3 = Vec3(0.0, 0.0, 0.0),
    var angle: Float = 45f,
    var range: Float = 20f,
    var cookie: Int = -1,
) : AbstractLight()

data class AreaLight(
    var dir: Vec3 = Vec3(0.0, 0.0, 0.0),
    var size: Vec2 = Vec2(1f, 1f),
    var range: Float = 5f,
    var cookie: Int = -1,
) : AbstractLight()
