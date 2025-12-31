package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks
import net.minecraft.core.*
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes

class GreyboxBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::GreyboxBlock)!!
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        Shapes.create(0.2, 0.0, 0.2, 0.8, 0.95, 0.8)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Greybox.entityType!!.create(pos, state)!!
}