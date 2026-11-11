package com.flooferland.showbiz.commands

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object RootCommand : Command(Showbiz.MOD_ID) {
    override val showHelpOnRun = true
    override fun description() = Component.translatable("text.mod.description")
        .append(Component.literal("\n(You probably intended to use the subcommands)").withStyle(ChatFormatting.GRAY))!!
    override val children = Subcommands(
        BitmapCommand,
        WikiCommand,
        StatCommand,
        ConfigCommand,
        ReloadCommand
    )
    override fun run(ctx: CommandContext) = Response.empty()
}
