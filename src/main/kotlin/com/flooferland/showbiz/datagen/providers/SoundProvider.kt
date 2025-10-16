package com.flooferland.showbiz.datagen.providers

import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.rl
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

object SoundProvider {
    fun generateSound(sound: ModSounds): JsonObject? {
        return buildJsonObject {
            putJsonArray("sounds") {
                for (name in sound.sounds) {
                    add(
                        rl(name).withPrefix("${sound.folder}/").toString()
                    )
                }
            }
        }
    }
}