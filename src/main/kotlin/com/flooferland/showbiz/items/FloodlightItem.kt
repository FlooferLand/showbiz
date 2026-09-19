package com.flooferland.showbiz.items

import net.minecraft.network.chat.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.ShowbizUtils

class FloodlightItem(properties: Properties) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level as? ServerLevel ?: return InteractionResult.SUCCESS
        val player = context.player ?: return InteractionResult.PASS

        val placeState = level.getBlockState(context.clickedPos.above())
        val canPlaceOnBlock = placeState.isAir || !placeState.isCollisionShapeFullBlock(level, context.clickedPos.above())
        if (canPlaceOnBlock && context.hand == InteractionHand.MAIN_HAND) {
            val stack = context.itemInHand
            val floodlight = stack.get(ModComponents.Floodlight.type) ?: return InteractionResult.PASS
            val entity = FloodlightEntity(level, floodlight)
            entity.setPos(context.clickLocation)
            context.rotation.let { yaw ->
                entity.xRot = 0f
                entity.yRot = yaw
                entity.yRotO = yaw
                entity.yHeadRot = yaw
                entity.yHeadRotO = yaw
                entity.yBodyRot = yaw
                entity.yBodyRotO = yaw
            }
            level.addFreshEntity(entity)
            player.setItemInHand(context.hand, ItemStack.EMPTY)
        }
        return InteractionResult.PASS
    }

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag)
        ShowbizUtils.itemTooltip(ModItems.Floodlight.id, tooltip)
    }
}