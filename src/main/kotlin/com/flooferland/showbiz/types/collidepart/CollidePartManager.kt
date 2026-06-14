package com.flooferland.showbiz.types.collidepart

import net.minecraft.world.level.*
import com.flooferland.showbiz.types.OwnerId

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
        fun tick(level: Level, ownerId: OwnerId) {}
        fun refresh(level: Level, ownerId: OwnerId) {}
    }
}