package com.flooferland.showbiz.registry

import net.minecraft.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.commands.BitmapCommand
import com.flooferland.showbiz.commands.StatCommand
import com.flooferland.showbiz.commands.WikiCommand
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object ModCommands {
    object RootCommand : Command(Showbiz.MOD_ID) {
        override val description = Component.translatable("text.mod.description")
            .append(Component.literal("\n(You probably intended to use the subcommands)").withStyle(ChatFormatting.GRAY))!!
        override val children = Subcommands(
            BitmapCommand,
            WikiCommand,
            StatCommand
        )
        override fun run(ctx: CommandContext) = Response.success(description)
    }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(RootCommand.build())
        }
    }
}