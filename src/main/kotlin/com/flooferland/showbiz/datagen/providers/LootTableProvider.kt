package com.flooferland.showbiz.datagen.providers

import net.minecraft.core.*
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModDecoBlocks
import java.util.concurrent.CompletableFuture
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider

class LootTableProvider(output: FabricDataOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) : FabricBlockLootTableProvider(output, registryLookup) {
    override fun generate() {
        for (deco in ModDecoBlocks.entries) {
            val drop = when (deco.type) {
                ModDecoBlocks.Type.Other -> null
                ModDecoBlocks.Type.Door -> createDoorTable(deco.block)
            }
            if (drop != null)
                add(deco.block) { drop }
        }
    }

    companion object {
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
}