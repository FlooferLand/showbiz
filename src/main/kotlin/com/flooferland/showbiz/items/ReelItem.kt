package com.flooferland.showbiz.items

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.Extensions.applyComponent
import net.minecraft.*
import net.minecraft.network.chat.*
import net.minecraft.world.item.*

class ReelItem(properties: Properties) : Item(properties) {
    override fun getName(stack: ItemStack): Component? {
        val item = Component.translatable(getDescriptionId(stack))
        var boundFile = stack.components.get(ModComponents.FileName.type) ?: return item
        if (boundFile.isEmpty()) boundFile = "Empty"
        return item.copy().append(
            Component.literal(" (${boundFile.replace(".rshw", "")})")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC)
        )
    }

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        if (tooltipFlag.isCreative) return

        val boundFile = stack.components.get(ModComponents.FileName.type)
        if (boundFile == null || boundFile.length < 2) {
            tooltip.add(Component.literal("No file bound.").withStyle(ChatFormatting.GRAY))
            tooltip.add(
                Component.literal("Use ")
                .append(Component.literal("/${Showbiz.MOD_ID} reelUpload").withStyle(ChatFormatting.GREEN))
                .append(" to upload to this tape").withStyle(ChatFormatting.GRAY)
            )
            return
        }

        tooltip.add(Component.literal("File: $boundFile").withStyle(ChatFormatting.GRAY))
    }

    companion object {
        fun makeItem(filename: String): ItemStack {
            val reelStack = ItemStack(ModItems.Reel.item)
            reelStack.applyComponent(ModComponents.FileName.type, filename)
            return reelStack
        }
    }
}