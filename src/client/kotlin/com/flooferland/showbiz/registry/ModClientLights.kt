package com.flooferland.showbiz.registry

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.light.*
import com.flooferland.showbiz.types.lights.BaseLightBackend
import com.flooferland.showbiz.types.lights.IRLightBackend
import com.flooferland.showbiz.types.lights.VanillaLightBackend
import com.flooferland.showbiz.types.lights.VeilLightBackend
import com.flooferland.showbiz.utils.Extensions.forceLoad
import com.flooferland.showbiz.utils.ShowbizEnv
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import java.util.concurrent.ConcurrentHashMap

object ModClientLights {
    val lights = ConcurrentHashMap<OwnerId, AbstractLight>()
    var backend = findBackend()
        public get
        private set

    fun findBackend(): BaseLightBackend {
        val config = Showbiz.config.light
        if (config.forceVanillaLighting) return VanillaLightBackend

        val allowIRLights = ShowbizEnv.hasIRLights() && !com.flooferland.showbiz.types.IRLights.shadersDisabled()
        val allowVeil = ShowbizEnv.hasVeil()

        fun loadError(thing: String, e: Throwable): BaseLightBackend? {
            Showbiz.log.error("Failed to load $thing backend: ", e)
            return null
        }

        val found = when {
            config.preferVeil && allowVeil -> runCatching { VeilLightBackend }.getOrElse { loadError("Veil", it) }
            config.preferIRLights && allowIRLights -> runCatching { IRLightBackend }.getOrElse { loadError("IRLights", it) }
            allowVeil -> runCatching { VeilLightBackend }.getOrElse { loadError("Veil", it) }
            allowIRLights -> runCatching { IRLightBackend }.getOrElse { loadError("IRLights", it) }
            else -> null
        } ?: VanillaLightBackend

        Showbiz.log.debug("Switching to light backend '{}'", found::class.simpleName)
        return found
    }

    fun load() {
        VanillaLightBackend::class.forceLoad()
        reload()

        LightManager.addLight = { ownerId, light ->
            lights[ownerId] = light
            Minecraft.getInstance().execute { backend.addLight(ownerId, light) }
        }

        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, _ ->
            if (entity is ILightEmitter) {
                val ownerId = OwnerId.ofEntity(entity)
                lights[ownerId]?.let { backend.removeLight(ownerId, it) }
                lights.remove(ownerId)
            }
        }
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            if (entity is ILightEmitter) {
                val ownerId = OwnerId.ofEntity(entity)
                lights[ownerId]?.let { backend.removeLight(ownerId, it) }
                lights.remove(ownerId)
            }
        }
        ClientTickEvents.END_WORLD_TICK.register { level ->
            // Cleaning up lights
            lights.entries.removeIf { (ownerId, light) ->
                if (ownerId.isRemoved(level)) {
                    backend.removeLight(ownerId, light)
                    return@removeIf true
                }
                false
            }
        }

        // Reloading
        InvalidateRenderStateCallback.EVENT.register {
            Minecraft.getInstance().execute { reload() }
        }

        // Remaking all the lights
        ClientPlayConnectionEvents.DISCONNECT.register { listener, minecraft ->
            minecraft.execute { reload() }
        }
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { minecraft, level ->
            minecraft.execute { reload() }
        }
    }

    fun emit(level: ClientLevel, delta: Float) {
        for ((ownerId, light) in lights) {
            val owner = ownerId.grabLightEmitter(level) ?: continue
            owner.clientUpdateLights(delta)

            backend.preRender(ownerId, light)
            when (light) {
                is PointLight -> backend.renderPointlight(ownerId, light)
                is SpotLight -> backend.renderSpotlight(ownerId, light)
                is AreaLight -> backend.renderArealight(ownerId, light)
            }
            backend.postRender(ownerId, light)
        }
    }

    /** Updates the backend and reloads everything */
    fun reload() {
        val newBackend = findBackend()
        if (backend != newBackend) {
            lights.forEach { (ownerId, light) -> backend.removeLight(ownerId, light) }
            backend.unload()
            backend.reset()

            newBackend.reset()
            newBackend.load()
            backend = newBackend
            lights.forEach { (ownerId, light) -> backend.addLight(ownerId, light) }
        }
    }
}