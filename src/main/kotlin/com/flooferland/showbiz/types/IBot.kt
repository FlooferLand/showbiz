package com.flooferland.showbiz.types

import net.minecraft.world.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.types.connection.ConnectionPort
import com.flooferland.showbiz.types.connection.data.PackedShowData

interface IBot {
    public var botId: ResourceId?
    public val botOwnerId: OwnerId? get
    public val botLevel: Level? get
    public val botPos: Vec3? get

    public val show: ConnectionPort<PackedShowData>?
}