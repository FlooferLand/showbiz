package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks
import net.minecraft.core.*
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.BlockHitResult
import com.flooferland.showbiz.registry.ModItems

class BitViewBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::BitViewBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.BitView.entityType!!.create(pos, state)!!

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        if (player.isHolding(ModItems.Wand.item)) return InteractionResult.PASS
        player.openMenu(getMenuProvider(state, level, pos))
        return InteractionResult.SUCCESS
    }
}