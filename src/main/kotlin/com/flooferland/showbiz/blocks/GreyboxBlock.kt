package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState

class GreyboxBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::GreyboxBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Greybox.entityType!!.create(pos, state)!!
}