package com.flooferland.showbiz.types

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity

interface IBotSoundHandler {
    fun tick(entity: StagedBotBlockEntity, level: Level, pos: BlockPos, state: BlockState)
}