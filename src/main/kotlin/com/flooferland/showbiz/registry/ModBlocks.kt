package com.flooferland.showbiz.registry

import com.flooferland.showbiz.blocks.GreyboxBlock
import com.flooferland.showbiz.blocks.ReelToReelBlock
import com.flooferland.showbiz.blocks.StagedBotBlock
import com.flooferland.showbiz.blocks.entities.GreyboxBlockEntity
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.datagen.providers.BlockProvider.BlockModelId
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.BlockBehaviour.*

enum class ModBlocks {
    StagedBot(
        "staged_bot", ::StagedBotBlock,
        Properties.of()
            .strength(5.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.METAL)
            .noOcclusion(),
        entity = ::StagedBotBlockEntity
    ),
    ReelToReel(
        "reel_to_reel", ::ReelToReelBlock,
        Properties.of()
            .strength(5.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.METAL)
            .noOcclusion(),
        entity = ::ReelToReelBlockEntity
    ),
    Greybox(
        "greybox", ::GreyboxBlock,
        Properties.of()
            .strength(3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.METAL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = ::GreyboxBlockEntity
    );

    val id: ResourceLocation
    val block: Block
    val item: BlockItem
    var model: BlockModelId? = null
    var entity: BlockEntityType<*>? = null
    constructor(name: String, constructor: (Properties) -> Block, props: Properties, modelPreset: BlockModelId = BlockModelId.CubeAll, entity: ((pos: BlockPos, blockState: BlockState) -> BlockEntity)? = null) {
        this.id = rl(name)
        this.model = modelPreset;

        this.block = Blocks.register(
            ResourceKey.create(BuiltInRegistries.BLOCK.key(), this.id),
            //? if >1.21.9 {
            /*constructor, props
            *///?} else {
            constructor(props)
            //?}
        )
        this.item = Items.registerBlock(this.block) as BlockItem

        if (entity != null && !DataGenerator.engaged) {
            this.entity = BlockEntityType.Builder.of(entity, this.block).build()
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, this.id, this.entity!!)
        }
    }
}