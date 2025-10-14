package com.flooferland.showbiz.registry

import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.datagen.providers.ItemProvider.ItemModelId
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties

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

        this.item = constructor(props)
        Registry.register(BuiltInRegistries.ITEM, this.id, this.item)
    }
}