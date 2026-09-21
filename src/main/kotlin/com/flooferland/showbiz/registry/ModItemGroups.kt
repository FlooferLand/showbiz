package com.flooferland.showbiz.registry

import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.network.chat.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.utils.rl

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
        main.add(ModRecipes.MitziPlush.outputProvider())
        main.add(ModRecipes.MiniPlush.outputProvider())
        main.add(ModRecipes.DookPlush.outputProvider())
        main.add(ModRecipes.GullyDookPlush.outputProvider())

        for (block in ModDecoBlocks.entries) {
            deco.add(block.item.defaultInstance)
        }
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