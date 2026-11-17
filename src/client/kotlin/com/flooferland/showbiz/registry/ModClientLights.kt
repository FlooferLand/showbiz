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
    val lights = ConcurrentHashMap<ILightEmitter, AbstractLight>()
    var backend = findBackend()
        public get
        private set

    enum class BackendType { Vanilla, Veil, IRLights }

    fun findBackend(): BaseLightBackend {
        val config = Showbiz.config.light
        if (config.forceVanillaLighting) return VanillaLightBackend

        val allowIRLights = ShowbizEnv.hasIRLights() && !com.flooferland.showbiz.types.IRLights.shadersDisabled()
        val allowVeil = ShowbizEnv.hasVeil()

        fun get(type: BackendType, load: () -> BaseLightBackend): BaseLightBackend? =
            runCatching { load() }.getOrElse { Showbiz.log.error("Failed to load the $type backend: ", it); null }

        val backend = when {
            config.preferVeil && allowVeil -> get(BackendType.Veil) { VeilLightBackend }
            config.preferIRLights && allowIRLights -> get(BackendType.IRLights) { IRLightBackend }
            allowVeil -> get(BackendType.Veil) { VeilLightBackend }
            allowIRLights -> get(BackendType.IRLights) { IRLightBackend }
            else -> null
        } ?: VanillaLightBackend

        Showbiz.log.debug("Switching to light backend '{}'", backend::class.simpleName)
        return backend
    }

    fun load() {
        VanillaLightBackend::class.forceLoad()
        reload()

        // Adding lights
        LightManager.addLight = addLight@ { emitter, light ->
            addLight(emitter)
        }
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register { entity, _ ->
            val emitter = entity as? ILightEmitter ?: return@register
            addLight(emitter)
        }
        ClientEntityEvents.ENTITY_LOAD.register { entity, _ ->
            val emitter = entity as? ILightEmitter ?: return@register
            addLight(emitter)
        }

        // Cleaning up lights
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, _ ->
            val emitter = entity as? ILightEmitter ?: return@register
            removeLight(emitter)
        }
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            val emitter = entity as? ILightEmitter ?: return@register
            removeLight(emitter)
        }
        ClientTickEvents.END_WORLD_TICK.register { level ->
            lights.entries.removeIf { (emitter, light) ->
                val ownerId = OwnerId.ofLight(emitter)
                if (ownerId?.isRemoved(level) ?: true) {
                    backend.removeLight(emitter, light)
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

    fun addLight(emitter: ILightEmitter, light: AbstractLight? = null) {
        val light = light ?: emitter.lightManager.lights.firstOrNull() ?: return
        if (!lights.containsKey(emitter)) {
            lights[emitter] = light
            Minecraft.getInstance().execute { backend.addLight(emitter, light) }
        }
    }

    fun removeLight(emitter: ILightEmitter) {
        lights[emitter]?.let { backend.removeLight(emitter, it) }
        lights.remove(emitter)
    }

    fun emit(level: ClientLevel, delta: Float) {
        for ((emitter, light) in lights) {
            val owner = OwnerId.ofLight(emitter)?.grabLightEmitter(level) ?: continue
            owner.clientUpdateLights(delta)

            backend.preRender(emitter, light)
            when (light) {
                is PointLight -> backend.renderPointlight(emitter, light)
                is SpotLight -> backend.renderSpotlight(emitter, light)
                is AreaLight -> backend.renderArealight(emitter, light)
            }
            backend.postRender(emitter, light)
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