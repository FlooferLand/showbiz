package com.flooferland.showbiz.datagen.providers

import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.blockPath
import com.flooferland.showbiz.utils.rlVanilla
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

object BlockProvider {
    enum class BlockModelId {
        CubeAll,
        BlockEntity,
        Custom;

        var data: JsonObject? = null
        companion object {
            fun withData(data: JsonObject): BlockModelId =
                BlockModelId.Custom.also { it.data = data }
        }
    }

    fun generateBlockModel(block: ModBlocks): JsonObject? {
        return when (block.model!!) {
            BlockModelId.CubeAll -> buildJsonObject {
                put("parent", rlVanilla("cube_all").blockPath().toString())
                putJsonObject("textures") {
                    put("all", block.id.blockPath().toString())
                }
            }
            BlockModelId.BlockEntity -> null
            BlockModelId.Custom -> null
        }
    }

    fun generateBlockItemModel(block: ModBlocks): JsonObject? {
        return buildJsonObject {
            put("parent", block.id.blockPath().toString())
        }
    }

    fun generateState(block: ModBlocks): JsonObject? {
        return when (block.model!!) {
            BlockModelId.BlockEntity, BlockModelId.CubeAll -> buildJsonObject {
                putJsonObject("variants") {
                    putJsonObject("") {
                        put("model", block.id.blockPath().toString())
                    }
                }
            }
            BlockModelId.Custom -> null
        }
    }
}