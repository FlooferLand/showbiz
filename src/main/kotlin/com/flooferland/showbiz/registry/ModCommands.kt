package com.flooferland.showbiz.registry

import com.flooferland.showbiz.commands.RootCommand
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object ModCommands {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(RootCommand.build())
        }
    }
}