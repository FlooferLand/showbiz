package com.flooferland.showbiz.items

import net.minecraft.network.chat.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.ShowbizUtils

class BotItem(properties: Properties) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level as? ServerLevel ?: return InteractionResult.SUCCESS
        val player = context.player ?: return InteractionResult.PASS

        val placeState = level.getBlockState(context.clickedPos.above())
        val canPlaceOnBlock = placeState.isAir || !placeState.isCollisionShapeFullBlock(level, context.clickedPos.above())
        if (canPlaceOnBlock && context.hand == InteractionHand.MAIN_HAND) {
            val stack = context.itemInHand
            val botId = stack.get(ModComponents.BotId.type) ?: run { // Default bot
                Showbiz.bots.keys
                    .sortedWith(compareBy({ it.namespace != MOD_ID }, { it.toString() }))
                    .firstOrNull()
            }
            println("Found bot $botId")
            if (botId == null) {
                player.displayClientMessage(Component.literal("Failed to select a default bot"), true)
                return InteractionResult.SUCCESS
            }
            val entity = BotEntity(level, botId)
            entity.setPos(context.clickLocation)
            entity.yRot = 180f + context.rotation
            level.addFreshEntity(entity)
            player.setItemInHand(context.hand, ItemStack.EMPTY)
        }
        return InteractionResult.PASS
    }

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag)
        ShowbizUtils.itemTooltip(ModItems.Bot.id, tooltip)
    }
}