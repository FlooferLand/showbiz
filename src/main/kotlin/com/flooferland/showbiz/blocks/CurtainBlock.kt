package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.registry.ModBlocks

class CurtainBlock(props: Properties) : BaseEntityBlock(props) {
    val codec = simpleCodec(::CurtainBlock)!!
    override fun codec() = codec

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.CurtainBlock.entityType!!.create(pos, state)!!
}