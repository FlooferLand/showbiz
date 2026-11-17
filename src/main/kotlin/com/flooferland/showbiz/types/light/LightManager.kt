package com.flooferland.showbiz.types.light

import com.flooferland.showbiz.types.OwnerId

class LightManager(val entity: ILightEmitter) {
    fun <T: AbstractLight> add(light: T): T? {
        val ownerId = OwnerId.ofLight(entity) ?: return null
        addLight(ownerId, light)
        return light
    }

    companion object {
        // Filled on the client
        var addLight: (owner: OwnerId, light: AbstractLight) -> Unit = { _, _ -> }
    }
}