package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.Extensions.generateHorizontal
import com.flooferland.showbiz.utils.Extensions.getCachedOrRotate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

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

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        return shapes.getCachedOrRotate(facing)
    }

    companion object {
        val shapes: HashMap<Direction, VoxelShape> = run {
            var shape = Shapes.box(0.0, 0.0, 0.0625, 1.0, 1.0, 1.0);
            shape.generateHorizontal()
        }
    }
}