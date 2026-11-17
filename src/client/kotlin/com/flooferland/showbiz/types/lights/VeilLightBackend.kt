package com.flooferland.showbiz.types.lights

import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.light.AbstractLight
import com.flooferland.showbiz.types.light.AreaLight
import com.flooferland.showbiz.types.light.PointLight
import com.flooferland.showbiz.types.light.SpotLight
import foundry.veil.api.client.render.VeilRenderSystem
import foundry.veil.api.client.render.light.data.AreaLightData
import foundry.veil.api.client.render.light.data.LightData
import foundry.veil.api.client.render.light.data.PointLightData
import foundry.veil.api.client.render.light.renderer.LightRenderHandle
import foundry.veil.fabric.event.FabricVeilRenderLevelStageEvent
import net.minecraft.util.Mth
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.concurrent.ConcurrentHashMap

internal object VeilLightBackend : BaseLightBackend() {
    val handles = ConcurrentHashMap<OwnerId, LightRenderHandle<*>>()

    init {
        FabricVeilRenderLevelStageEvent.EVENT.register { stage, renderer, source, stack, fc, fc2, i, tracker, camera, frustum ->

        }
    }

    override fun reset() {
        handles.values.forEach { it.free() }
        handles.clear()
    }

    override fun addLight(ownerId: OwnerId, light: AbstractLight) {
        val data: LightData = when (light) {
            is PointLight -> PointLightData()
            is SpotLight -> runCatching { foundry.veil.api.client.render.light.data.SpotLightData() }.getOrNull() ?: AreaLightData()
            is AreaLight -> AreaLightData()
        }
        veilConfigureLight(ownerId, data, light)

        val handle = VeilRenderSystem.renderer().getLightRenderer().addLight(data)
        handles[ownerId] = handle
    }

    override fun removeLight(ownerId: OwnerId, light: AbstractLight) {
        handles[ownerId]?.free()
        handles.remove(ownerId)
    }

    override fun postRender(ownerId: OwnerId, light: AbstractLight) {
        val handle = handles[ownerId] ?: return
        veilConfigureLight(ownerId, handle.lightData, light)
    }

    private fun veilConfigureLight(ownerId: OwnerId, data: LightData, light: AbstractLight) {
        if (level == null) return
        val inscattering = 3f
        val brightness = light.intensity * if (light.enabled) 5f else 0f
        val occlusion = light.shadows
        val pos = getPos(ownerId, light) ?: return
        val up = Vector3f(0f, 1f, 0f)

        data.setColor(light.color.pack())

        when (light) {
            is PointLight -> if (data is PointLightData) {
                runCatching { data.inscatteringStrength = inscattering }
                data.brightness = brightness
                data.radius = light.radius
                data.setOcclusionEnabled(occlusion)

                data.position = Vector3d(pos.toVector3f())
            }

            is AreaLight -> if (data is AreaLightData) {
                runCatching { data.inscatteringStrength = 0f }
                data.brightness = brightness
                data.distance = light.range
                data.setSize(light.size.x.toDouble() * 0.48f, light.size.y.toDouble() * 0.48f)  // "In blocks" MY ASS
                data.setOcclusionEnabled(occlusion)

                val direction = light.dir.toVector3f().negate()
                data.position.set(Vector3d(pos.toVector3f()))
                data.orientation.set(Quaternionf().identity().lookAlong(direction, up))
            }

            // NOTE: Spotlights only exist in modern Veil
            is SpotLight -> runCatching { if (data is foundry.veil.api.client.render.light.data.SpotLightData) {
                data.inscatteringStrength = inscattering
                data.brightness = brightness
                data.distance = light.range

                data.size = light.angle * Mth.DEG_TO_RAD
                data.angle = 10f * Mth.DEG_TO_RAD
                data.setOcclusionEnabled(occlusion)

                val direction = light.dir.toVector3f().negate()
                data.positionMutable.set(Vector3d(pos.toVector3f()))
                data.orientationMutable.set(Quaternionf().identity().lookAlong(direction, up))
            } }
        }
    }
}