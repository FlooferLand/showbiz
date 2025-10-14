package com.flooferland.showbiz.datagen.providers

import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.Extensions.itemPath
import com.flooferland.showbiz.utils.rlVanilla
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

object ItemProvider {
    enum class ItemModelId {
        Generated,
        Custom;

        var data: JsonObject? = null
        companion object {
            fun withData(data: JsonObject): ItemModelId =
                ItemModelId.Custom.also { it.data = data }
        }
    }

    fun generateItem(item: ModItems): JsonObject? {
        return when (item.model!!) {
            ItemModelId.Generated -> buildJsonObject {
                put("parent", rlVanilla("generated").itemPath().toString())
                putJsonObject("textures") {
                    put("layer0", item.id.itemPath().toString())
                }
            }
            ItemModelId.Custom -> null
        }
    }
}