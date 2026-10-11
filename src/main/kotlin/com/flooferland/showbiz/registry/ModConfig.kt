package com.flooferland.showbiz.registry

import net.minecraft.server.level.*
import com.akuleshov7.ktoml.Toml
import com.flooferland.showbiz.Showbiz
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.fabricmc.loader.api.FabricLoader

@Serializable
enum class PermissionSelector(private val rank: Int) {
    Operator(0),
    CreativeMode(1),
    SurvivalMode(2),
    Anyone(3)
    ;
    fun test(player: ServerPlayer): Boolean {
        if (!player.mayBuild()) return false
        val playerRank = when {
            player.hasPermissions(player.server.operatorUserPermissionLevel) -> Operator.rank
            player.gameMode.isCreative -> CreativeMode.rank
            player.gameMode.isSurvival -> SurvivalMode.rank
            else -> Anyone.rank
        }
        return playerRank <= rank
    }
}

@Serializable
data class ModConfig(val audio: Audio = Audio(), val permissions: Permissions = Permissions(), val other: Other = Other()) : Cloneable {
    @Serializable
    data class Audio(
        var playPneumaticSounds: Boolean = true,
        var playBotEffects: Boolean = true
    )

    @Serializable
    data class Permissions(
        var writeReels: PermissionSelector = PermissionSelector.Anyone,
        var switchReels: PermissionSelector = PermissionSelector.Anyone,
        var controlPlayback: PermissionSelector = PermissionSelector.Anyone,
        var editScreenAccess: PermissionSelector = PermissionSelector.Anyone,
    )

    @Serializable
    data class Other(
        var forceVanillaLighting: Boolean = false
    )

    public override fun clone() = super.clone() as ModConfig

    fun save() {
        try {
            val file = getCommonPath().toFile()
            file.writeText(Toml.encodeToString<ModConfig>(this))
            Showbiz.log.info("Saved config to '${file.path}'")
        } catch (e: Exception) {
            Showbiz.log.error("Error saving config", e)
        }
    }

    companion object {
        fun getCommonPath(): Path = FabricLoader.getInstance().configDir.resolve("${Showbiz.MOD_ID}.toml")
        fun load(): ModConfig {
            var config = Showbiz.config.clone()
            try {
                val file = getCommonPath().toFile()
                if (file.exists()) {
                    config = Toml.decodeFromString<ModConfig>(file.readText())
                } else {
                    file.writeText(Toml.encodeToString<ModConfig>(config))
                }
                Showbiz.log.info("Loaded config from '${file.path}'")
            } catch (e: Exception) {
                Showbiz.log.error("Error loading config", e)
            }
            return config
        }
    }
}