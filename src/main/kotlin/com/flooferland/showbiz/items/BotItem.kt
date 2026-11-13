package com.flooferland.showbiz.items

import com.flooferland.showbiz.Permissions.Companion.mayPlaceAt
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.types.IStackPlacePreview
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.utils.ShowbizUtils
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level

class BotItem(properties: Properties) : Item(properties), IStackPlacePreview {
    override fun getEntity(level: Level, stack: ItemStack): Entity? {
        return BotEntity(level, stack.getOrDefault(ModComponents.BotId.type, firstBotIdOrDefault()))
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level ?: return InteractionResult.SUCCESS
        val player = context.player ?: return InteractionResult.PASS
        val pos = context.clickedPos.above()
        val stack = context.itemInHand
        if (!player.mayPlaceAt(level, pos, stack)) return InteractionResult.FAIL

        val placeState = level.getBlockState(pos)
        val canPlaceOnBlock = placeState.isAir || !placeState.isCollisionShapeFullBlock(level, pos)
        if (canPlaceOnBlock && context.hand == InteractionHand.MAIN_HAND) {
            val botId = stack.get(ModComponents.BotId.type) ?: firstBotIdOrDefault()
            if (level is ServerLevel) {
                val entity = BotEntity(level, botId)
                placeEntity(entity, context.clickLocation, player)
                level.addFreshEntity(entity)
            }
            player.setItemInHand(context.hand, ItemStack.EMPTY)
        }
        return InteractionResult.SUCCESS
    }

    override fun getDefaultInstance(): ItemStack =
        ItemStack(this).also { it.set(ModComponents.BotId.type, firstBotIdOrDefault()) }

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

    companion object {
        // Two defaults since defaultBotId should probably be nullable
        fun firstBotIdOrDefault(): ResourceId =
            firstBotIdOrNull() ?: ResourceId("showbiz", "beach_bear")
        fun firstBotIdOrNull(): ResourceId? =
            Showbiz.bots.keys
                .sortedWith(compareBy({ it.namespace != MOD_ID }, { it.toString() }))
                .firstOrNull()
    }
}