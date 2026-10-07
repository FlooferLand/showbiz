package com.flooferland.showbiz.types.commands.base

import net.minecraft.network.chat.*
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext

open class CommandContainer(name: String) : Command(name) {
    override val description = Component.literal("You probably meant to use a sub-command")!!
    override fun run(ctx: CommandContext) = Response.empty()
}