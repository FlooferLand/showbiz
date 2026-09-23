package com.flooferland.showbiz.items

import net.minecraft.core.*
import net.minecraft.network.chat.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.ShowbizUtils
import kotlin.math.round

class FloodlightItem(properties: Properties) : Item(properties) {
    override fun useOn(ctx: UseOnContext): InteractionResult {
        val level = ctx.level as? ServerLevel ?: return InteractionResult.SUCCESS
        val player = ctx.player ?: return InteractionResult.PASS

        val canPlaceOnBlock = (ctx.clickedFace == Direction.DOWN || ctx.clickedFace == Direction.UP)
        if (ctx.hand == InteractionHand.MAIN_HAND && canPlaceOnBlock) {
            val stack = ctx.itemInHand
            val floodlight = stack.get(ModComponents.Floodlight.type) ?: return InteractionResult.PASS
            floodlight.turn.x = round(ctx.rotation * 100f) / 100f
            val entity = FloodlightEntity(level, floodlight)

            val pos = if (ctx.clickedFace == Direction.DOWN) ctx.clickLocation.subtract(0.0, entity.bbHeight.toDouble(), 0.0) else ctx.clickLocation
            entity.setPos(pos)
            entity.supportBlock = ctx.clickedPos
            entity.supportBlockLoc = ctx.clickLocation
            level.addFreshEntity(entity)
            player.setItemInHand(ctx.hand, ItemStack.EMPTY)
        }
        return InteractionResult.PASS
    }

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag)
        ShowbizUtils.itemTooltip(ModItems.Floodlight.id, tooltip)
    }
}