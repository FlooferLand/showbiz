package com.flooferland.showbiz.types.connection

import net.minecraft.world.entity.*
import net.minecraft.world.level.*
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
    fun grabLevel(): Level? = when (this) {
        is BlockEntity -> if (this.hasLevel()) this.getLevel() else null
        is Entity -> this.level()
        else -> null
    }
    fun grabRemoved(): Boolean = when (this) {
        is BlockEntity -> this.isRemoved
        is Entity -> this.isRemoved
        else -> true
    }
}