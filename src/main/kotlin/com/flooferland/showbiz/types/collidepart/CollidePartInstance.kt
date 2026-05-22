package com.flooferland.showbiz.types.collidepart

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

class CollidePartInstance(val owner: ICollidePartInteractable, val init: BonePrepare.() -> Unit, val clientInstance: CollidePartManager.IInstance?) : CollidePartManager.IInstance {
    val bonesToIds = mutableMapOf<String, CollidePartId>()

    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) clientInstance?.tick(level, pos, state)
        if (bonesToIds.isEmpty()) refresh(level, pos)
    }

    override fun refresh(level: Level, pos: BlockPos) {
        if (level.isClientSide) clientInstance?.refresh(level, pos)
        val prepare = BonePrepare()
        init(prepare)
    }

    inner class BonePrepare() {
        public fun map(bone: String, id: CollidePartId) = bonesToIds.put(bone, id)
    }
}