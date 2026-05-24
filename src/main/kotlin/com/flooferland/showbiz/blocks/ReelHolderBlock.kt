package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks

class ReelHolderBlock(props: Properties) : FacingEntityBlock(props) {
    val shape = Shapes.box(0.375, 0.0, 0.375, 0.625, 0.03125, 0.625)!!

    override val codec = simpleCodec(::ReelHolderBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) = shape
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.ReelHolder.entityType!!.create(pos, state)!!

}