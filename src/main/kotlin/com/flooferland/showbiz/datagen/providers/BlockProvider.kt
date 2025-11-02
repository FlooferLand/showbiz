package com.flooferland.showbiz.datagen.providers

import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.blockPath
import com.flooferland.showbiz.utils.rl
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
        var transparent = false
        fun transparent(): BlockModelId { transparent = true; return this }
    }

    fun generateBlockModel(block: ModBlocks, model: CustomBlockModel.Model): JsonObject? {
        val customModel = (block.block as? CustomBlockModel)?.modelData()
        customModel?.let { return@generateBlockModel customModel }

        return when (block.model!!) {
            BlockModelId.CubeAll -> buildJsonObject {
                put("parent", rlVanilla("cube_all").blockPath().toString())
                putJsonObject("textures") {
                    val texName = if (model.textures.isEmpty()) block.id.blockPath() else model.textures.first()
                    put("all", texName.toString())
                }
            }
            BlockModelId.BlockEntity -> null
            BlockModelId.Custom -> null
        }
    }

    // TODO: Accumulate every separate variant together so every state include severy variant as the string
    fun generateStates(block: ModBlocks, variations: List<CustomBlockModel.Variation>): JsonObject? {
        return buildJsonObject {
            putJsonObject("variants") {
                if (variations.isEmpty()) {
                    putJsonObject("") {
                        put("model", block.id.blockPath().toString())
                    }
                } else for (variation in variations) {
                    putJsonObject("${variation.prop.name}=${variation.expected}") {
                        put("model", rl(variation.name.name!!).blockPath().toString())
                        if (variation.state.x != 0) put("x", variation.state.x)
                        if (variation.state.y != 0) put("y", variation.state.y)
                    }
                }
            }
        }
    }

    fun generateBlockItemModel(block: ModBlocks, state: CustomBlockModel.StateModel): JsonObject? {
        return buildJsonObject {
            put("parent", rl(state.name.toString()).blockPath().toString())
        }
    }
}