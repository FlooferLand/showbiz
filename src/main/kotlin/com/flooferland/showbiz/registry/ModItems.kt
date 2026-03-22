package com.flooferland.showbiz.registry

import com.flooferland.showbiz.components.OptionBlockPos
import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.datagen.providers.ItemProvider.ItemModelId
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.*

enum class ModItems {
    Wand(
        "wand", ::WandItem,
        { stacksTo(1).component(ModComponents.WandBind.type, OptionBlockPos.EMPTY) },
        model = ItemModelId.Custom,
        recipe = ModRecipes.Wand
    ),
    Reel(
        "reel", ::ReelItem,
        { stacksTo(1).component(ModComponents.FileName.type, "") },
        model = ItemModelId.Custom,
        recipe = ModRecipes.Reel
    )
    ;

    val id: ResourceLocation
    lateinit var item: Item
    var model: ItemModelId? = null
    var hideFromPlayer: Boolean = false
    var recipe: ModRecipes? = null
    constructor(name: String, constructor: (Properties) -> Item, properties: Properties.() -> Properties, model: ItemModelId = ItemModelId.Generated, hideFromPlayer: Boolean = false, recipe: ModRecipes?) {
        this.id = rl(name)
        this.model = model;
        this.hideFromPlayer = hideFromPlayer
        this.recipe = recipe
        if (DataGenerator.engaged) return

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