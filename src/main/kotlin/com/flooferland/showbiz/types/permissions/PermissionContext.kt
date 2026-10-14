package com.flooferland.showbiz.types.permissions

import net.minecraft.server.*
import net.minecraft.server.level.*

class PermissionContext(val server: MinecraftServer, val player: ServerPlayer) {
    val isOp get() = server.playerList.isOp(player.gameProfile)
}