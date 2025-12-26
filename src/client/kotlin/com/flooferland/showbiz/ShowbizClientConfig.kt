package com.flooferland.showbiz

import kotlinx.serialization.Serializable

@Serializable
data class ShowbizClientConfig(val audio: Audio = Audio()) : Cloneable {
    @Serializable
    data class Audio(
        var playPneumaticSounds: Boolean = true
    )

    public override fun clone() = super.clone() as ShowbizClientConfig
}