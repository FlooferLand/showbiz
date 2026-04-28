package com.flooferland.showbiz.registry

import kotlinx.serialization.Serializable

@Serializable
data class ModConfig(val audio: Audio = Audio(), val permissions: Permissions = Permissions()) : Cloneable {
    @Serializable
    data class Audio(
        var playPneumaticSounds: Boolean = true,
        var playBotEffects: Boolean = true
    )

    @Serializable
    data class Permissions(
        val restrict: Boolean = false
    )

    public override fun clone() = super.clone() as ModConfig
}