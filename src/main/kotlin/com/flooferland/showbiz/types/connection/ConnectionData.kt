package com.flooferland.showbiz.types.connection

import com.flooferland.showbiz.types.IPacketable
import com.flooferland.showbiz.types.OwnerId
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

abstract class ConnectionData<T: ConnectionData<T>>(val typeId: String) : IPacketable {
    abstract fun tempReset()

    open fun canSendTo(ownerId: OwnerId, player: ServerPlayer): Boolean = true
    open fun canReach(origin: Vec3, player: ServerPlayer): Boolean = true

    /**
     * Merges [other] into itself. Returns true if the merge was successful, false if self should be replaced. <br/>
     * WARNING: This can lead to a memory leak very quickly!!
     */
    open fun merge(other: T): Boolean {
        return false
    }
}