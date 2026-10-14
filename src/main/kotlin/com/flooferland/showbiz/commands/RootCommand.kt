package com.flooferland.showbiz.commands

import net.minecraft.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext

object RootCommand : Command(Showbiz.MOD_ID) {
    override val showHelpOnRun = true
    override fun description() = Component.translatable("text.mod.description")
        .append(Component.literal("\n(You probably intended to use the subcommands)").withStyle(ChatFormatting.GRAY))!!
    override val children = Subcommands(
        BitmapCommand,
        WikiCommand,
        StatCommand,
        ConfigCommand
    )
    override fun run(ctx: CommandContext) = Response.empty()
}
