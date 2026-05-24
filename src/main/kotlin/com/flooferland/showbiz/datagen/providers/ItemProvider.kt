package com.flooferland.showbiz.datagen.providers

import net.minecraft.resources.*
import com.flooferland.showbiz.utils.Extensions.itemPath
import com.flooferland.showbiz.utils.rlVanilla
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

object ItemProvider {
    enum class ItemModelId {
        Generated,
        MusicDisc,
        Custom;

        var data: JsonObject? = null
        companion object {
            fun withData(data: JsonObject): ItemModelId =
                ItemModelId.Custom.also { it.data = data }
        }
    }

    fun generateModel(model: ItemModelId, id: ResourceLocation): JsonObject? {
        return when (model) {
            ItemModelId.Generated -> buildJsonObject {
                put("parent", rlVanilla("generated").itemPath().toString())
                putJsonObject("textures") {
                    put("layer0", id.itemPath().toString())
                }
            }
            ItemModelId.MusicDisc -> buildJsonObject {
                put("parent", rlVanilla("template_music_disc").itemPath().toString())
                putJsonObject("textures") {
                    put("layer0", id.itemPath().toString())
                }
            }
            ItemModelId.Custom -> null
        }
    }

    fun generateItem(id: ResourceLocation): JsonObject? {
        return buildJsonObject {
            putJsonObject("model") {
                put("type", rlVanilla("model").toString())
                put("model", id.toString())
            }
        }
    }
}