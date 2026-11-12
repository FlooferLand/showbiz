package com.flooferland.showbiz.registry

import com.flooferland.showbiz.utils.rl
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack

enum class ModItemGroups {
    Main("main", ModRecipes.MitziPlush, { main, deco ->
        for (block in ModBlocks.entries) {
            if (!block.hideFromPlayer) main.add(block.item.defaultInstance)
        }
        for (item in ModItems.entries) {
            if (!item.hideFromPlayer) main.add(item.item.defaultInstance)
        }
        for (disc in ModMusicDiscs.entries) {
            main.add(disc.item.defaultInstance)
        }

        // Plushies
        main.add(ModRecipes.MitziPlush.outputProvider())
        main.add(ModRecipes.MiniPlush.outputProvider())
        main.add(ModRecipes.DookPlush.outputProvider())
        main.add(ModRecipes.GullyDookPlush.outputProvider())

        //region Deco
        for (entry in ModDecoBlocks.children) {
            deco.add(entry.item.defaultInstance)
        }

        // Props
        deco.add(ModRecipes.MoonshineProp.outputProvider())
        deco.add(ModRecipes.LargeRockProp.outputProvider())
        deco.add(ModRecipes.MediumRockProp.outputProvider())
        deco.add(ModRecipes.SmallRockProp.outputProvider())
        //endregion
    });

    enum class Section {
        Main,
        Deco
    }

    val tab: CreativeModeTab
    val items: List<ItemStack>
    val sections: Map<ItemStack, Section>
    constructor(name: String, icon: ModRecipes, generator: (MutableList<ItemStack>, MutableList<ItemStack>) -> Unit) {
        val mainSection = mutableListOf<ItemStack>()
        val decoSection = mutableListOf<ItemStack>()
        generator(mainSection, decoSection)

        sections = mutableMapOf<ItemStack, Section>().also { map ->
            mainSection.forEach { map[it] = Section.Main }
            decoSection.forEach { map[it] = Section.Deco }
        }
        items = mainSection + decoSection
        tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.showbiz.$name"))
            .icon { icon.outputProvider() }
            .displayItems { _, output -> items.forEach { output.accept(it) } }
            .build()
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, rl(name), tab)
    }
}