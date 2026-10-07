package com.flooferland.showbiz

import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents

/** NOTE: This should only be called on the server */
object ServerStats {
    val reelToReels = hashSetOf<ReelToReelBlockEntity>()

    fun init() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register { entity, level ->
            if (entity is ReelToReelBlockEntity)
                reelToReels.add(entity)
        }
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
            reelToReels.remove(entity)
        }
    }
}