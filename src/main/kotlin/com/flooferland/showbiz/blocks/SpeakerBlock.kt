package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.generateHorizontal
import com.flooferland.showbiz.utils.Extensions.getCachedOrRotate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class SpeakerBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::SpeakerBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Speaker.entityType!!.create(pos, state)!!

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