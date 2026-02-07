package com.flooferland.showbiz.blocks.base

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block

/** Like [BlockItem], but with proper custom tooltip support */
class FancyBlockItem(val id: ResourceLocation, block: Block, properties: Properties) : BlockItem(block, properties) {
    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag)

        // Custom tooltip
        val comp = Component.translatableWithFallback("tooltip.${id.namespace}.block.${id.path}", "")
        if (comp.string.isEmpty()) return
        for (text in comp.string.split('\n')) {
            var comp = Component.literal(text).withStyle(ChatFormatting.GRAY)
            if (text.startsWith('(') && text.endsWith(')')) {
                comp = comp.withStyle(ChatFormatting.DARK_GRAY)
            }
            tooltip.add(comp)
        }
    }
}