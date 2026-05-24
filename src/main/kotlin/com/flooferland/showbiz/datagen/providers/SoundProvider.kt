package com.flooferland.showbiz.datagen.providers

import net.minecraft.resources.*
import kotlinx.serialization.json.*

object SoundProvider {
    fun generateSound(sounds: Array<ResourceLocation>, folder: String? = null, procedural: Boolean = false, stream: Boolean = false): JsonObject? {
        return buildJsonObject {
            putJsonArray("sounds") {
                if (procedural) {
                    addJsonObject {
                        put("name", "fabric-sound-api-v1:empty")
                        put("stream", true)
                    }
                } else {
                    for (id in sounds) {
                        val id = if (folder != null) { id.withPrefix("${folder}/") } else id
                        addJsonObject {
                            put("name", id.toString())
                            put("stream", stream)
                        }
                    }
                }
            }
        }
    }
}