package com.flooferland.showbiz.types.light

interface ILightEmitter {
    val lightManager: LightManager
    fun clientUpdateLights(delta: Float)
}