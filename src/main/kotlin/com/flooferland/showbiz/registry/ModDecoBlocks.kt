package com.flooferland.showbiz.registry

import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockBehaviour.*
import com.flooferland.showbiz.blocks.base.ShowbizDoorBlock
import com.flooferland.showbiz.items.base.FancyBlockItem
import com.flooferland.showbiz.utils.rl

// NOTE: Could probably add the ability to pass in a datagen generator for making the model
//       Ex: { g -> g.createDoor(it) }
enum class ModDecoBlocks {
    RedDoor(
        "red_door", Type.Door,
        ::ShowbizDoorBlock
    );

    enum class Type {
        Other,
        Door
    }

    val id: ResourceLocation
    val type: Type
    val block: Block
    val item: BlockItem
    val transparent: Boolean
    constructor(name: String, type: Type, constructor: (Properties) -> Block) {
        this.id = rl(name)
        this.type = type

        // Block
        val props = Properties.of()
            .strength(0.8f)
            .noOcclusion()
        this.block = Blocks.register(
            ResourceKey.create(BuiltInRegistries.BLOCK.key(), this.id),
            //? if >1.21.9 {
            /*constructor, props
            *///?} else {
            constructor(props)
            //?}
        )
        this.transparent = type == Type.Door

        // Item
        var blockItem = FancyBlockItem(this.id, this.block, Item.Properties())
        this.item = Items.registerBlock(blockItem) as BlockItem
    }
}