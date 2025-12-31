package com.flooferland.showbiz.types

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.registry.ModBlocks

object ModelPartManager {
    var modelPartData: ModelPartMap? = null
    var modelPartInstancer: (IModelPartInteractable, block: ModBlocks) -> IInstance = { _, _ -> object : IInstance {} }

    @Suppress("UNCHECKED_CAST")
    fun create(owner: IModelPartInteractable, block: ModBlocks) = modelPartInstancer(owner, block)

    fun getMaxReach(player: Player) = if (player.isCreative) 4f else 2.7f

    interface IInstance {
        fun kill() {}
        fun tick(level: Level, pos: BlockPos, state: BlockState) {}
    }
}