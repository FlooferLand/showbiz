package com.flooferland.showbiz.items.base

import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.*
import com.flooferland.showbiz.utils.ShowbizUtils

/** Like [net.minecraft.world.item.BlockItem], but with proper custom tooltip support */
open class FancyBlockItem(val id: ResourceLocation, block: Block, properties: Properties) : BlockItem(block, properties) {
    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag)
        ShowbizUtils.blockTooltip(id, tooltip)
    }
}