package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks
import net.minecraft.core.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*

class GreyboxBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::GreyboxBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Greybox.entity!!.create(pos, state)!!
}