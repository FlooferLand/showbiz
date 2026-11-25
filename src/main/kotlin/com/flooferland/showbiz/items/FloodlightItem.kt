package com.flooferland.showbiz.items

import com.flooferland.showbiz.Permissions.Companion.mayPlaceAt
import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.items.base.FancyItem
import com.flooferland.showbiz.registry.ModComponents
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import kotlin.math.round

class FloodlightItem(id: ResourceLocation, properties: Properties) : FancyItem(id, properties) {
    override fun useOn(ctx: UseOnContext): InteractionResult {
        val level = ctx.level as? ServerLevel ?: return InteractionResult.SUCCESS
        val player = ctx.player ?: return InteractionResult.PASS
        val pos = ctx.clickedPos
        val stack = ctx.itemInHand
        if (!player.mayPlaceAt(level, pos, stack)) return InteractionResult.FAIL

        val canPlaceOnBlock = (ctx.clickedFace == Direction.DOWN || ctx.clickedFace == Direction.UP)
        if (ctx.hand == InteractionHand.MAIN_HAND && canPlaceOnBlock) {
            val floodlight = stack.get(ModComponents.Floodlight.type) ?: return InteractionResult.PASS
            floodlight.turn.x = round(ctx.rotation * 100f) / 100f
            val entity = FloodlightEntity(level, floodlight)

            val clickedPos = if (ctx.clickedFace == Direction.DOWN) ctx.clickLocation.subtract(0.0, entity.bbHeight.toDouble(), 0.0) else ctx.clickLocation
            entity.setPos(clickedPos)
            entity.supportBlock = pos
            entity.supportBlockLoc = ctx.clickLocation
            level.addFreshEntity(entity)
            player.setItemInHand(ctx.hand, ItemStack.EMPTY)
        }
        return InteractionResult.PASS
    }
}