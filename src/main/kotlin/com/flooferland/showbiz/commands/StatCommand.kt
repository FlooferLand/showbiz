package com.flooferland.showbiz.commands

import net.minecraft.network.chat.*
import com.flooferland.showbiz.commands.stat.StatShowsCommand
import com.flooferland.showbiz.types.commands.base.CommandContainer

object StatCommand : CommandContainer("stat") {
    override val description = Component.literal("Lets you view things happening in your world/server")!!
    override val children = Subcommands(
        StatShowsCommand
    )
}
