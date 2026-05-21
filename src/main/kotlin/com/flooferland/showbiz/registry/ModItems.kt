package com.flooferland.showbiz.registry

import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.*
import com.flooferland.showbiz.components.OptionBlockPos
import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.datagen.providers.ItemProvider.ItemModelId
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.utils.rl

enum class ModItems {
    Wand(
        "wand", ::WandItem,
        { stacksTo(1).component(ModComponents.BlockOwner.type, OptionBlockPos.EMPTY) },
        model = ItemModelId.Custom
    ),
    Reel(
        "reel", ::ReelItem,
        { stacksTo(1).component(ModComponents.FileName.type, "") },
        model = ItemModelId.Custom
    )
    ;

    val id: ResourceLocation
    val item: Item
    var model: ItemModelId? = null
    var hideFromPlayer: Boolean = false
    constructor(name: String, constructor: (Properties) -> Item, properties: Properties.() -> Properties, model: ItemModelId = ItemModelId.Generated, hideFromPlayer: Boolean = false) {
        this.id = rl(name)
        this.model = model;
        this.hideFromPlayer = hideFromPlayer

        val baseProps = Properties()
        this.item = Items.registerItem(
            ResourceKey.create(BuiltInRegistries.ITEM.key(), this.id),
            //? if >1.21.9 {
            /*constructor, properties(baseProps)
            *///?} else {
            constructor(properties(baseProps))
            //?}
        )
    }
}