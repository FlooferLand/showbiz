package com.flooferland.showbiz.types.light

class LightManager(val entity: ILightEmitter) {
    val lights = mutableSetOf<AbstractLight>()

    fun <T: AbstractLight> add(light: T): T? {
        addLight(entity, light)
        lights += light
        return light
    }

    companion object {
        // Filled on the client
        var addLight: (emitter: ILightEmitter, light: AbstractLight) -> Unit = { _, _ -> }
    }
}