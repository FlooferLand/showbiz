package com.flooferland.showbiz.items

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.Extensions.applyComponent
import net.minecraft.*
import net.minecraft.network.chat.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.*
import net.minecraft.world.level.Level
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

class ReelItem(properties: Properties) : Item(properties) {
    override fun getName(stack: ItemStack): Component? {
        val item = Component.translatable(getDescriptionId(stack))
        var boundFile = stack.components.get(ModComponents.FileName.type) ?: return item
        if (boundFile.isEmpty()) boundFile = "Empty"
        return item.copy().append(
            Component.literal(" (${Path(boundFile).nameWithoutExtension})")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC)
        )
    }

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        if (tooltipFlag.isCreative) return

        val boundFile = stack.components.get(ModComponents.FileName.type)
        if (boundFile == null || boundFile.length < 2) {
            tooltip.add(Component.literal("No file bound.").withStyle(ChatFormatting.GRAY))
            tooltip.add(Component.empty())
            tooltip.add(
                Component.literal("Right-click the air to upload to this reel").withStyle(ChatFormatting.GRAY)
            )
            tooltip.add(
                Component.literal("Or use ")
                    .append(Component.literal("/${Showbiz.MOD_ID} reelupload").withStyle(ChatFormatting.GREEN))
            )
            return
        }

        tooltip.add(Component.literal("File: $boundFile").withStyle(ChatFormatting.GRAY))
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (level.isClientSide) openScreenClient(stack)
        return InteractionResultHolder.success(stack)
    }

    companion object {
        var openScreenClient: (ItemStack) -> Unit = { }

        fun makeItem(filename: String): ItemStack {
            val reelStack = ItemStack(ModItems.Reel.item)
            reelStack.applyComponent(ModComponents.FileName.type, filename)
            return reelStack
        }
    }
}