package com.flooferland.showbiz

import net.minecraft.core.*
import net.minecraft.server.level.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.pattern.*
import com.flooferland.showbiz.registry.PermissionSelector
import com.flooferland.showbiz.utils.Extensions.notifyPermissionError

// TODO: Sync permissions from the server over to the client so its on the same page inside `test`
enum class Permissions(var selector: PermissionSelector) {
    WriteReels(Showbiz.config.permissions.writeReels),
    SwitchReels(Showbiz.config.permissions.switchReels),
    ControlPlayback(Showbiz.config.permissions.controlPlayback),
    EditScreenAccess(Showbiz.config.permissions.editScreenAccess),
    ;

    fun test(player: Player): Boolean {
        val serverTest = (player as? ServerPlayer)?.let { selector.test(it) } ?: true
        return player.mayBuild() && serverTest
    }
    fun testAndNotify(player: Player): Boolean =
        test(player).also { if (!it) player.notifyPermissionError(this) }

    companion object {
        // Will fill these later
        fun Player.mayInteractWith(entity: BlockEntity): Boolean {
            return mayInteractAt(entity.blockPos)
        }
        fun Player.mayInteractAt(pos: BlockPos): Boolean {
            return mayBuild() && mayInteract(level(), pos)
        }
        fun Player.mayInteractWith(entity: Entity): Boolean {
            return mayBuild() && !entity.isRemoved
        }
        fun Player.mayHurt(entity: Entity): Boolean {
            return mayInteractWith(entity)
        }

        fun Player.mayBuildAt(pos: BlockPos): Boolean {
            val blockPlaceRestricted = (this as? ServerPlayer)?.let { it.blockActionRestricted(it.level(), pos, it.gameMode.gameModeForPlayer) } ?: false
            return mayBuild() && !blockPlaceRestricted
        }
        fun Player.mayUseItemOn(level: Level, blockPos: BlockPos, stack: ItemStack): Boolean {
            if (mayBuild()) return true
            val blockInWorld = BlockInWorld(this.level(), blockPos, false)
            return stack.canPlaceOnBlockInAdventureMode(blockInWorld)
        }
    }
}