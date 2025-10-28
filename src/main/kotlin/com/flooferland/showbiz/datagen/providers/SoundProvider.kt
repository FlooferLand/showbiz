package com.flooferland.showbiz.datagen.providers

import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.rl
import kotlinx.serialization.json.*

object SoundProvider {
    fun generateSound(sound: ModSounds): JsonObject? {
        return buildJsonObject {
            putJsonArray("sounds") {
                if (sound.procedural) {
                    addJsonObject {
                        put("name", "fabric-sound-api-v1:empty")
                        put("stream", true)
                    }
                } else {
                    for (name in sound.sounds) {
                        add(rl(name).withPrefix("${sound.folder}/").toString())
                    }
                }
            }
        }
    }
}