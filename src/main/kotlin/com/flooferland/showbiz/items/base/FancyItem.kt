package com.flooferland.showbiz.items.base

import com.flooferland.showbiz.utils.ShowbizUtils
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

/** Like [net.minecraft.world.item.Item], but with proper custom tooltip support */
open class FancyItem(val id: ResourceLocation, properties: Properties) : Item(properties) {
    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag)
        ShowbizUtils.itemTooltip(id, tooltip)
    }
}