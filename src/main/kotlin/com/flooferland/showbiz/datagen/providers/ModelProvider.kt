package com.flooferland.showbiz.datagen.providers

import net.minecraft.data.models.*
import com.flooferland.showbiz.registry.ModDecoBlocks
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider

class ModelProvider(output: FabricDataOutput) : FabricModelProvider(output) {
    override fun generateBlockStateModels(generator: BlockModelGenerators) {
        for (deco in ModDecoBlocks.entries) {
            when (deco.type) {
                ModDecoBlocks.Type.Other -> {}
                ModDecoBlocks.Type.Door -> generator.createDoor(deco.block)
            }
        }
    }

    override fun generateItemModels(generator: ItemModelGenerators) {
    }
}