package com.flooferland.showbiz.types

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.registry.ModBlocks

/** Responsible for initializing parts */
object ModelPartManager {
    var modelPartData: ModelPartMap? = null
    var clientModelPartInstancer: (IModelPartInteractable, block: ModBlocks) -> IInstance? = { _, _ -> null }

    @Suppress("UNCHECKED_CAST")
    fun create(owner: IModelPartInteractable, block: ModBlocks) =
        ModelPartInstance(owner, block, clientModelPartInstancer(owner, block))

    fun getMaxReach(player: Player) = if (player.isCreative) 4f else 2.7f

    interface IInstance {
        fun kill() {}
        fun tick(level: Level, pos: BlockPos, state: BlockState) {}
    }
}