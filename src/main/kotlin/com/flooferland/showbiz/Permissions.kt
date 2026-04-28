package com.flooferland.showbiz

import net.minecraft.server.level.ServerPlayer

object Permissions {
    fun canWriteReels(player: ServerPlayer) =
        if (Showbiz.config.permissions.restrict)
            player.isCreative || player.hasPermissions(player.server.operatorUserPermissionLevel)
        else true
}