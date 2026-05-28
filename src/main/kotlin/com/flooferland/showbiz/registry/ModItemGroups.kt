package com.flooferland.showbiz.registry

import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.network.chat.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.utils.rl

enum class ModItemGroups {
    Main("main", { params, out ->
        for (block in ModBlocks.entries) {
            if (!block.hideFromPlayer) out.accept(block.item)
        }
        for (item in ModItems.entries) {
            if (!item.hideFromPlayer) out.accept(item.item)
        }
        for (disc in ModMusicDiscs.entries) {
            out.accept(disc.item)
        }
        out.accept(ModRecipes.MitziPlush.outputProvider())
        out.accept(ModRecipes.DookPlush.outputProvider())
    });

    constructor(name: String, generator: CreativeModeTab.DisplayItemsGenerator) {
        val group = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.showbiz.$name"))
            .icon { ModRecipes.MitziPlush.outputProvider() }
            .displayItems(generator)
            .build()
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, rl(name), group)
    }
}