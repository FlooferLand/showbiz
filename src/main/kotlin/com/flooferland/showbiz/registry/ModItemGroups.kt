package com.flooferland.showbiz.registry

import com.flooferland.showbiz.utils.rl
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab

enum class ModItemGroups {
    Main("main", { params, out ->
        out.accept(ModBlocks.TestStage.item)
        out.accept(ModItems.Wand.item)
    });

    constructor(name: String, generator: CreativeModeTab.DisplayItemsGenerator) {
        val group = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.showbiz.$name"))
            .icon({ ModBlocks.TestStage.item.defaultInstance })
            .displayItems(generator)
            .build()
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, rl(name), group)
    }
}