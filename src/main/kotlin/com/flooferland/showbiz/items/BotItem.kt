package com.flooferland.showbiz.items

import com.flooferland.showbiz.Permissions.Companion.mayPlaceAt
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.types.IStackPlacePreview
import com.flooferland.showbiz.utils.ShowbizUtils
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level

class BotItem(properties: Properties) : Item(properties), IStackPlacePreview {
    override fun getEntity(level: Level, stack: ItemStack) =
        BotEntity(level, stack.get(ModComponents.BotId.type) ?: defaultBotId())

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level ?: return InteractionResult.SUCCESS
        val player = context.player ?: return InteractionResult.PASS
        val pos = context.clickedPos.above()
        val stack = context.itemInHand
        if (!player.mayPlaceAt(level, pos, stack)) return InteractionResult.FAIL

        val placeState = level.getBlockState(pos)
        val canPlaceOnBlock = placeState.isAir || !placeState.isCollisionShapeFullBlock(level, pos)
        if (canPlaceOnBlock && context.hand == InteractionHand.MAIN_HAND) {
            val botId = stack.get(ModComponents.BotId.type) ?: defaultBotId()
            if (botId == null) {
                player.displayClientMessage(Component.literal("Failed to select a default bot"), true)
                return InteractionResult.SUCCESS
            }
            if (level is ServerLevel) {
                val entity = BotEntity(level, botId)
                placeEntity(entity, context.clickLocation, player)
                level.addFreshEntity(entity)
            }
            player.setItemInHand(context.hand, ItemStack.EMPTY)
        }
        return InteractionResult.SUCCESS
    }

    fun defaultBotId() = Showbiz.bots.keys
        .sortedWith(compareBy({ it.namespace != MOD_ID }, { it.toString() }))
        .firstOrNull()

    override fun getName(stack: ItemStack): Component {
        val comp = Component.empty().append(super.getName(stack))
        stack.get(ModComponents.BotId.type)?.let { botId ->
            comp.append(Component.literal(" (${botId.path})").withStyle(ChatFormatting.GRAY))
        }
        return comp
    }

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag)
        ShowbizUtils.itemTooltip(ModItems.Bot.id, tooltip)
    }
}