package com.flooferland.showbiz.types.connection

import net.minecraft.server.level.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.phys.*

interface IConnectable {
    val connectionManager: ConnectionManager

    fun connectionChanged() {}

    fun grabCenterPos(): Vec3? = when (this) {
        is BlockEntity -> this.blockPos.center
        is Entity -> this.position()
        else -> null
    }
    fun grabLevel(): ServerLevel? = when (this) {
        is BlockEntity -> if (this.hasLevel()) this.getLevel() as? ServerLevel else null
        is Entity -> this.level() as? ServerLevel
        else -> null
    }
    fun grabRemoved(): Boolean = when (this) {
        is BlockEntity -> this.isRemoved
        is Entity -> this.isRemoved
        else -> true
    }
}