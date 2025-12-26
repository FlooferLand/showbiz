package com.flooferland.showbiz.registry

import net.minecraft.client.Minecraft
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

import net.minecraft.commands.*
import net.minecraft.network.chat.Component
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.screens.ShowbizConfigScreen
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

object ModClientCommands {
    private fun registerClientCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>, registry: CommandBuildContext): Array<LiteralArgumentBuilder<FabricClientCommandSource?>?> {
        val clientConfig = ClientCommandManager.literal("config")
            .executes { ctx ->
                Minecraft.getInstance().setScreen(ShowbizConfigScreen())
                0
            }

        return arrayOf(clientConfig)
    }

    init {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registry ->
            var command = ClientCommandManager.literal(Showbiz.MOD_ID + "client")
                .executes { ctx ->
                    ctx.source.sendFeedback(
                        Component.translatable("text.mod.description")
                            .append("\n(You probably intended to use the subcommands)")
                    )
                    1
                }
            for (entry in registerClientCommands(dispatcher,  registry)) {
                command = command.also { it.then(entry) }
            }
            dispatcher.register(command)
        }
    }
}