package com.flooferland.showbiz.datagen.providers

import net.minecraft.core.*
import net.minecraft.data.models.*
import net.minecraft.data.models.blockstates.*
import net.minecraft.data.models.model.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.properties.*
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModDecoBlocks
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider

class ModelProvider(output: FabricDataOutput) : FabricModelProvider(output) {
    override fun generateBlockStateModels(generator: BlockModelGenerators) {
        makeDiode(generator, ModBlocks.ShowParser.block)

        // Decoration blocks
        for (deco in ModDecoBlocks.entries) {
            when (deco.type) {
                ModDecoBlocks.Type.Other -> {}
                ModDecoBlocks.Type.Door -> generator.createDoor(deco.block)
            }
        }
    }

    override fun generateItemModels(generator: ItemModelGenerators) {
        generator.generateFlatItem(ModBlocks.ShowParser.item, ModelTemplates.FLAT_ITEM)
    }

    fun makeDiode(generator: BlockModelGenerators, block: Block) {
        val facing = PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
            .select(Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
            .select(Direction.SOUTH, Variant.variant())
            .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
            .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))

        generator.blockStateOutput.accept(
            MultiVariantGenerator.multiVariant(block)
                .with(BlockModelGenerators.createBooleanModelDispatch(
                    DiodeBlock.POWERED,
                    ModelLocationUtils.getModelLocation(block, "_on"),
                    ModelLocationUtils.getModelLocation(block, "_off")
                ))
                .with(facing)
        )
    }
}