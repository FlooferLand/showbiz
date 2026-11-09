package com.flooferland.showbiz.registry

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.flooferland.showbiz.Showbiz
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.level.ServerPlayer
import java.nio.file.Path

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
data class ModConfig(val audio: Audio = Audio(), val permissions: Permissions = Permissions(), val addons: Addons = Addons(), val other: Other = Other()) : Cloneable {
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
        var editServerShowBridge: PermissionSelector = PermissionSelector.Operator,
    )

    @Serializable
    data class Addons(
        var showNoticeScreen: Boolean = true
    )

    @Serializable
    data class Other(
        var forceVanillaLighting: Boolean = false,
        var serverHighResVideo: Boolean = false
    )

    public override fun clone() = super.clone() as ModConfig

    fun save() {
        try {
            val file = getCommonPath().toFile()
            file.writeText(toml.encodeToString<ModConfig>(this))
            Showbiz.log.info("Saved config to '${file.path}'")
        } catch (e: Exception) {
            Showbiz.log.error("Error saving config", e)
        }
    }

    companion object {
        val toml = Toml(
            inputConfig = TomlInputConfig(ignoreUnknownNames = true),
            outputConfig = TomlOutputConfig(ignoreDefaultValues = true)
        )

        fun getCommonPath(): Path = FabricLoader.getInstance().configDir.resolve("${Showbiz.MOD_ID}.toml")
        fun load(): ModConfig {
            var config = Showbiz.config.clone()
            try {
                val file = getCommonPath().toFile()
                if (file.exists()) {
                    config = toml.decodeFromString<ModConfig>(file.readText())
                } else {
                    file.writeText(toml.encodeToString<ModConfig>(config))
                }
                Showbiz.log.info("Loaded config from '${file.path}'")
            } catch (e: Exception) {
                Showbiz.log.error("Error loading config", e)
            }
            return config
        }
    }
}