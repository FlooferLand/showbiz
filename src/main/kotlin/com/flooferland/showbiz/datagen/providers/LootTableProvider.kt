package com.flooferland.showbiz.datagen.providers

import com.flooferland.showbiz.registry.ModBlocks
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object LootTableProvider {
    fun generateBlockDrops(block: ModBlocks) =
        buildJsonObject {
            put("type", "minecraft:block")
            putJsonArray("pools") {
                addJsonObject {
                    put("rolls", 1)
                    putJsonArray("entries") {
                        addJsonObject {
                            put("type", "minecraft:item")
                            put("name", block.id.toString())
                            putJsonArray("conditions") {
                                addJsonObject { put("condition", "minecraft:survives_explosion") }
                            }
                        }
                    }
                }
            }
        }
}