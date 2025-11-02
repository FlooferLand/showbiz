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
        Properties().stacksTo(1)
            .component(ModComponents.WandBind.type, OptionBlockPos.EMPTY),
        model = ItemModelId.Custom
    ),
    Reel(
        "reel", ::ReelItem,
        Properties().stacksTo(1)
            .component(ModComponents.FileName.type, ""),
        model = ItemModelId.Custom
    )
    ;

    val id: ResourceLocation
    lateinit var item: Item
    var model: ItemModelId? = null
    constructor(name: String, constructor: (Properties) -> Item, properties: Properties, model: ItemModelId = ItemModelId.Generated) {
        this.id = rl(name)
        this.model = model;
        if (DataGenerator.engaged) return

        this.item = Items.registerItem(
            ResourceKey.create(BuiltInRegistries.ITEM.key(), this.id),
            //? if >1.21.9 {
            /*constructor, props
            *///?} else {
            constructor(properties)
            //?}
        )
    }
}