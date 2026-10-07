package com.flooferland.showbiz.commands

import net.minecraft.network.chat.*
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext
import com.flooferland.showbiz.utils.Extensions.asLink

object WikiCommand : Command("wiki") {
    override fun description() = Component.literal("Gives you a link to the wiki")!!
    override fun run(ctx: CommandContext): Response {
        return Response.success(Component.literal("https://github.com/FlooferLand/showbiz/wiki").asLink())
    }
}