package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.registry.ModBlocks
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class TestStageBlock(props: Properties) : BaseEntityBlock(props) {
    val codec: MapCodec<TestStageBlock> = simpleCodec(::TestStageBlock)

    override fun codec(): MapCodec<out BaseEntityBlock> = codec
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ModBlocks.TestStage.entity!!.create(pos, state)!!
    }
}