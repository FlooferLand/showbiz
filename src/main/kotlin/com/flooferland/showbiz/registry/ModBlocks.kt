package com.flooferland.showbiz.registry

import com.flooferland.showbiz.blocks.TestStageBlock
import com.flooferland.showbiz.blocks.entities.TestStageBlockEntity
import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.datagen.providers.BlockProvider.BlockModelId
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState

enum class ModBlocks {
    TestStage(
        "test_stage", ::TestStageBlock,
        Properties.of()
                .strength(5.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL),
        entity = ::TestStageBlockEntity
    );

    val id: ResourceLocation
    lateinit var block: Block
    lateinit var item: BlockItem
    var model: BlockModelId? = null
    var entity: BlockEntityType<*>? = null
    constructor(name: String, constructor: (Properties) -> Block, props: Properties, model: BlockModelId = BlockModelId.CubeAll, entity: ((pos: BlockPos, blockState: BlockState) -> BlockEntity)? = null) {
        this.id = rl(name)
        this.model = model;
        if (DataGenerator.engaged) return

        this.block = Blocks.register(
            ResourceKey.create(BuiltInRegistries.BLOCK.key(), this.id),
            //? if >1.21.9 {
            /*constructor, props
            *///?} else {
            constructor(props)
            //?}
        )
        this.item = Items.registerBlock(this.block) as BlockItem

        if (entity != null) {
            this.entity = BlockEntityType.Builder.of(entity, this.block).build()
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, this.id, this.entity!!)
        }
    }
}