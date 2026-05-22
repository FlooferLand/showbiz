package com.flooferland.showbiz.types.collidepart

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/** Responsible for initializing parts */
object CollidePartManager {
    var clientInstancer: (ICollidePartInteractable) -> IInstance? = { _ -> null }

    @Suppress("UNCHECKED_CAST")
    fun create(owner: ICollidePartInteractable, block: CollidePartInstance.BonePrepare.() -> Unit) =
        CollidePartInstance(
            owner,
            block,
            clientInstancer(owner)
        )

    interface IInstance {
        fun tick(level: Level, pos: BlockPos, state: BlockState) {}
        fun refresh(level: Level, pos: BlockPos) {}
    }
}