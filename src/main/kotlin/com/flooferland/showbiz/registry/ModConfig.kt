package com.flooferland.showbiz.registry

import com.flooferland.showbiz.Showbiz
import java.nio.file.Path
import kotlinx.serialization.Serializable
import net.fabricmc.loader.api.FabricLoader

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

    companion object {
        fun getCommonPath(): Path = FabricLoader.getInstance().configDir.resolve("${Showbiz.MOD_ID}.toml")
    }
}