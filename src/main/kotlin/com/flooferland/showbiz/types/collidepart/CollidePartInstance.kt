package com.flooferland.showbiz.types.collidepart

class CollidePartInstance(val owner: ICollidePartInteractable, val init: BonePrepare.() -> Unit, val clientInstance: CollidePartManager.IInstance?) : CollidePartManager.IInstance {
    val bonesToIds = mutableMapOf<String, CollidePartId>()

    override fun tick() {
        clientInstance?.tick()
        if (bonesToIds.isEmpty())
            refresh()
    }

    override fun refresh() {
        clientInstance?.refresh()
        val prepare = BonePrepare()
        init(prepare)
    }

    override fun kill() {
        clientInstance?.kill()
    }

    inner class BonePrepare() {
        public fun map(bone: String, id: CollidePartId) = bonesToIds.put(bone, id)
    }
}