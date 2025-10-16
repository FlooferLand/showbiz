package com.flooferland.showbiz.datagen.providers

import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.blocks.CustomBlockModel
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
        Custom
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

    fun generateStates(block: ModBlocks, states: List<CustomBlockModel.Variation>): JsonObject? {
        return buildJsonObject {
            putJsonObject("variants") {
                if (states.isEmpty()) {
                    putJsonObject("") {
                        put("model", block.id.blockPath().toString())
                    }
                } else for (state in states) {
                    putJsonObject("${state.prop.name}=${state.expected}") {
                        put("model", rl(state.name.name!!).blockPath().toString())
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