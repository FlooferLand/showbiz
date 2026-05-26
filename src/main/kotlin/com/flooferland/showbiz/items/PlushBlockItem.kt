package com.flooferland.showbiz.items

import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.*
import com.flooferland.showbiz.components.PlushComponent
import com.flooferland.showbiz.items.base.GeoBlockItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.utils.Extensions.applyComponent
import com.flooferland.showbiz.utils.rl

class PlushBlockItem(id: ResourceLocation, block: Block, properties: Properties) : GeoBlockItem(id, block, properties) {
    constructor(properties: Properties) : this(ModBlocks.Plush.id, ModBlocks.Plush.block, properties)
    override fun getName(stack: ItemStack): Component {
        val id = stack.get(ModComponents.Plush.type)?.id ?: return super.getName(stack)
        return Component.translatable("item.${id.namespace}.plush.${id.path}");
    }

    override fun getDefaultInstance() =
        ItemStack(this).also { it.applyComponent(ModComponents.Plush.type, PlushComponent(rl("mitzi"))) }
}