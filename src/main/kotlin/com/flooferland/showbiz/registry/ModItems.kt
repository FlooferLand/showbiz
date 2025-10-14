package com.flooferland.showbiz.registry

import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.datagen.providers.ItemProvider.ItemModelId
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.Items

enum class ModItems {
    Wand(
        "wand", ::Item,
        Properties()
    );

    val id: ResourceLocation
    lateinit var item: Item
    var model: ItemModelId? = null
    constructor(name: String, constructor: (Properties) -> Item, props: Properties, model: ItemModelId = ItemModelId.Generated) {
        this.id = rl(name)
        this.model = model;
        if (DataGenerator.engaged) return

        this.item = Items.registerItem(
            ResourceKey.create(BuiltInRegistries.ITEM.key(), this.id),
            constructor,
            props
        )
    }
}