package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.registry.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class TestStageBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.TestStage.entity!!, pos, blockState) {

}