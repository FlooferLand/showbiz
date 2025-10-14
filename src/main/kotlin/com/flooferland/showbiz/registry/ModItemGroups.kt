package com.flooferland.showbiz.registry

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.network.chat.Component

object ModItemGroups {
    val main = FabricItemGroup.builder()
        .title(Component.translatable("itemGroup.showbiz.main"))
        .icon({ ModBlocks.TestStage.item.defaultInstance })
        .displayItems { params, out ->
            out.accept(ModBlocks.TestStage.item)
            out.accept(ModItems.Wand.item)
        }
        .build()
}