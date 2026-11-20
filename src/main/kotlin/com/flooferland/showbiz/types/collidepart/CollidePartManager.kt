package com.flooferland.showbiz.types.collidepart

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
        fun tick() {}
        fun kill() {}
        fun refresh() {}
    }
}