package com.flooferland.showbiz.commands

import net.minecraft.network.chat.*
import com.flooferland.showbiz.commands.stat.StatShowsCommand
import com.flooferland.showbiz.types.commands.base.CommandContainer
import com.flooferland.showbiz.types.permissions.PermissionContext

object StatCommand : CommandContainer("stat") {
    override fun description() = Component.literal("Lets you view things happening in your world/server")!!
    override fun checkPermission(perms: PermissionContext) = perms.isOp
    override val children = Subcommands(
        StatShowsCommand
    )
}
