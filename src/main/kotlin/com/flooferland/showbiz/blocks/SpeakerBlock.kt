package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks
import net.minecraft.core.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*

class SpeakerBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::SpeakerBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Speaker.entityType!!.create(pos, state)!!
}