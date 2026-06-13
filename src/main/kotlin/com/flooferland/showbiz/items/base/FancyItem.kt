package com.flooferland.showbiz.items.base

import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.utils.ShowbizUtils

/** Like [net.minecraft.world.item.Item], but with proper custom tooltip support */
open class FancyItem(val id: ResourceLocation, properties: Properties) : Item(properties) {
    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag)
        ShowbizUtils.itemTooltip(id, tooltip)
    }
}