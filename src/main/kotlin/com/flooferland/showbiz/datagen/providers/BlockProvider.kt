package com.flooferland.showbiz.datagen.providers

import net.minecraft.resources.*
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
    // TODO: This entire thing should return to the same firey pits that birthed it
    enum class BlockModelType {
        CubeAll,
        BlockEntity,
        Custom,
        None;
        var transparent = false
        var noBlockState = false
        fun transparent(): BlockModelType { transparent = true; return this }
        fun noBlockState(): BlockModelType { noBlockState = true; return this }
    }
    data class BlockModelId(
        val type: BlockModelType,
        val transparent: Boolean = false,
        val noBlockState: Boolean = false
    ) {
        companion object {
            val CubeAll = BlockModelId(BlockModelType.CubeAll)
            val BlockEntity = BlockModelId(BlockModelType.BlockEntity)
            val Custom = BlockModelId(BlockModelType.Custom)
            val None = BlockModelId(BlockModelType.None)
        }
        fun transparent() = copy(transparent = true)
        fun noBlockState() = copy(noBlockState = true)
    }

    fun generateBlockModel(block: ModBlocks, model: CustomBlockModel.Model): JsonObject? {
        val customModel = (block.block as? CustomBlockModel)?.modelData()
        customModel?.let { return@generateBlockModel customModel }

        return when (block.model!!.type) {
            BlockModelType.CubeAll -> buildJsonObject {
                put("parent", rlVanilla("cube_all").blockPath().toString())
                putJsonObject("textures") {
                    val texName = if (model.textures.isEmpty()) block.id.blockPath() else model.textures.first()
                    put("all", texName.toString())
                }
            }
            BlockModelType.BlockEntity -> null
            BlockModelType.Custom -> null
            BlockModelType.None -> null
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
                        if (primary.name.name == null) {
                            error("Name is null for $primary (${block.id})")
                        }
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