package com.flooferland.showbiz.datagen.providers

import net.minecraft.resources.ResourceLocation
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.blockPath
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlVanilla
import com.google.common.collect.Lists.cartesianProduct
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

    // TODO: Accumulate every separate variant together so every state includes every variant in the string
    fun generateStates(block: ModBlocks, variations: List<CustomBlockModel.Variation>): JsonObject? {
        return buildJsonObject {
            putJsonObject("variants") {
                if (variations.isEmpty()) {
                    putJsonObject("") {
                        put("model", block.id.blockPath().toString())
                    }
                    return@putJsonObject
                }

                val props = variations.groupBy { it.prop.name }
                val propNames = props.keys.toList()
                val propsCombined = cartesianProduct(propNames.map { props[it] })
                for (combination in propsCombined) {
                    val key = combination.sortedBy { it.prop.name }.joinToString(",") { "${it.prop.name}=${it.expected}" }
                    val combinationMap = combination.associateBy({ it.prop.name }, { it.expected })
                    val primary = variations
                        .groupBy { it.name.name }
                        .mapNotNull { (_, sameNameVaris) ->
                            val matching = sameNameVaris.all { combinationMap[it.prop.name] == it.expected }
                            if (matching) sameNameVaris.size to sameNameVaris.first() else null
                        }
                        .maxByOrNull { it.first }
                        ?.second
                        ?: combination.first()
                    putJsonObject(key) {
                        put("model", rl(primary.name.name!!).blockPath().toString())
                        combination.sumOf { it.state.x }.let { if (it != 0) put("x", it) }
                        combination.sumOf { it.state.y }.let { if (it != 0) put("y", it) }
                    }
                }
            }
        }
    }

    fun generateBlockItemModel(block: ModBlocks, blockStateId: ResourceLocation): JsonObject? {
        return buildJsonObject {
            put("parent", blockStateId.toString())
        }
    }
}