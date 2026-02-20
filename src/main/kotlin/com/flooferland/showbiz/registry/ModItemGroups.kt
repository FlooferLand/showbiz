package com.flooferland.showbiz.registry

import com.flooferland.showbiz.utils.rl
import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.network.chat.*
import net.minecraft.world.item.*

enum class ModItemGroups {
    Main("main", { params, out ->
        for (block in ModBlocks.entries) {
            if (!block.hideFromSearch) out.accept(block.item)
        }
        for (item in ModItems.entries) {
            out.accept(item.item)
        }
    });

    constructor(name: String, generator: CreativeModeTab.DisplayItemsGenerator) {
        val group = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.showbiz.$name"))
            .icon({ ModBlocks.StagedBot.item.defaultInstance })
            .displayItems(generator)
            .build()
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, rl(name), group)
    }
}