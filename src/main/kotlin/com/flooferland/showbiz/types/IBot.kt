package com.flooferland.showbiz.types

import com.flooferland.showbiz.types.connection.ConnectionPort
import com.flooferland.showbiz.types.connection.data.PackedShowData
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

interface IBot {
    public var botId: ResourceId? get
    public var botVariant: String get
    public val botOwnerId: OwnerId? get
    public val botLevel: Level? get
    public val botPos: Vec3? get

    public val show: ConnectionPort<PackedShowData>?

    companion object {
        const val DEFAULT_VARIANT = "default"
    }
}