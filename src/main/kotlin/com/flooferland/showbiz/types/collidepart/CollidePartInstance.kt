package com.flooferland.showbiz.types.collidepart

import net.minecraft.world.level.*
import com.flooferland.showbiz.types.OwnerId

class CollidePartInstance(val owner: ICollidePartInteractable, val init: BonePrepare.() -> Unit, val clientInstance: CollidePartManager.IInstance?) : CollidePartManager.IInstance {
    val bonesToIds = mutableMapOf<String, CollidePartId>()

    override fun tick(level: Level, ownerId: OwnerId) {
        if (level.isClientSide) clientInstance?.tick(level, ownerId)
        if (bonesToIds.isEmpty()) refresh(level, ownerId)
    }

    override fun refresh(level: Level, ownerId: OwnerId) {
        if (level.isClientSide) clientInstance?.refresh(level, ownerId)
        val prepare = BonePrepare()
        init(prepare)
    }

    inner class BonePrepare() {
        public fun map(bone: String, id: CollidePartId) = bonesToIds.put(bone, id)
    }
}