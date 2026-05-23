package com.flooferland.showbiz.blocks

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.CymbalBlockEntity
import com.flooferland.showbiz.registry.ModBlocks

class CymbalBlock(properties: Properties) : FacingEntityBlock(properties) {
    override val codec = simpleCodec(::CymbalBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Cymbal.entityType!!.create(pos, state)!!
    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? CymbalBlockEntity)?.tick(level, pos, blockState) }
}