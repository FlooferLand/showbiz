package com.flooferland.showbiz.registry

import com.flooferland.showbiz.blocks.TestStageBlock
import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.datagen.providers.BlockProvider.BlockModelId
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour.Properties

enum class ModBlocks {
    TestStage(
        "test_stage", ::TestStageBlock,
        Properties.ofFullCopy(Blocks.IRON_BLOCK)
    );

    val id: ResourceLocation
    lateinit var block: Block
    lateinit var item: BlockItem
    var model: BlockModelId? = null
    constructor(name: String, constructor: (Properties) -> Block, props: Properties, model: BlockModelId = BlockModelId.CubeAll) {
        this.id = rl(name)
        this.model = model;
        if (DataGenerator.engaged) return

        this.block = constructor(props)
        this.item = BlockItem(block, Item.Properties())
        Registry.register(BuiltInRegistries.BLOCK, this.id, this.block)
        Registry.register(BuiltInRegistries.ITEM, this.id, this.item)
    }
}