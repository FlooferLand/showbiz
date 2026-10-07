package com.flooferland.showbiz

import net.minecraft.core.*
import net.minecraft.server.level.*
import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.registry.PermissionSelector

object Permissions {
    // Will fill these later
    fun ServerPlayer.mayInteractWith(entity: BlockEntity): Boolean {
        return mayInteractAt(entity.blockPos)
    }
    fun ServerPlayer.mayInteractAt(pos: BlockPos): Boolean {
        return mayBuild() && !blockActionRestricted(level(), pos, gameMode.gameModeForPlayer)
    }
    fun ServerPlayer.mayBuildAt(pos: BlockPos): Boolean {
        return mayBuild() && !blockActionRestricted(level(), pos, gameMode.gameModeForPlayer)
    }

    fun matches(player: ServerPlayer, selector: PermissionSelector): Boolean =
        player.mayBuild() && selector.test(player)

    fun canWriteReels(player: ServerPlayer) = matches(player, Showbiz.config.permissions.writeReels)
    fun canSwitchReels(player: ServerPlayer) = matches(player, Showbiz.config.permissions.switchReels)
    fun canControlPlayback(player: ServerPlayer) = matches(player, Showbiz.config.permissions.controlPlayback)
}