package com.flooferland.showbiz.registry

import com.flooferland.showbiz.FileStorage
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.items.ReelItem
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.CommandNode
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.*
import net.minecraft.commands.Commands.*
import net.minecraft.core.component.*
import net.minecraft.network.chat.*
import kotlin.io.path.Path
import kotlin.io.path.extension

object ModCommands {
    private fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>, registry: CommandBuildContext, environment: Commands.CommandSelection): Array<CommandNode<CommandSourceStack>> {
        val reelAdd = Commands.literal("reelUpload")
            .executes { ctx ->
                val shows = runCatching { FileStorage.fetchShows() }.onFailure { Showbiz.log.error(it.toString()) }.getOrNull()
                if (shows == null) {
                    ctx.source.sendFailure(Component.literal("No shows found"))
                    return@executes 0
                }

                val showsStr = shows.joinToString { "  - '${it}'\n" }
                ctx.source.sendSuccess(
                    { Component.literal("Call the command with one of the following file names: \n$showsStr") },
                    false
                )
                0
            }
            .then(
                argument("file", StringArgumentType.greedyString())
                    .executes { ctx ->
                        fun err(text: String): Int {
                            ctx.source.sendFailure(Component.literal(text))
                            return 0
                        }

                        val player = ctx.source.player ?: return@executes err("Player entity not found")
                        val heldItem = player.mainHandItem
                        if (heldItem?.isEmpty ?: true || heldItem.item !is ReelItem) {
                            return@executes err("You need to be holding a reel item to upload something to it")
                        }

                        // Validating the file
                        val filename = StringArgumentType.getString(ctx, "file") ?: return@executes err("Failed to find parameter")
                        if (!FileStorage.SUPPORTED_FORMATS.contains(Path(filename).extension)) {
                            return@executes err("File name must end with the supported formats: [${FileStorage.SUPPORTED_FORMATS.joinToString(", ") }]")
                        }

                        val shows = runCatching { FileStorage.fetchShows() }.onFailure { Showbiz.log.error(it.toString()) }.getOrNull()
                        if (shows == null || !shows.contains(filename)) {
                            ctx.source.sendFailure(
                                Component.literal("File does not exist: '${filename}'\n")
                                    .append(Component.literal("  Hint: \n"))
                                    .append(Component.literal(shows?.joinToString { "  - '${it}'\n" }.toString()))
                            )
                            return@executes 0
                        }

                        // Setting it to the item
                        heldItem.applyComponentsAndValidate(
                            DataComponentPatch.builder()
                                .set(ModComponents.FileName.type, filename)
                                .build()
                        )
                        1
                    }
            )
            .build()
        return arrayOf(reelAdd)
    }

    init {
        CommandRegistrationCallback.EVENT.register { dispatcher, registry, environment ->
            var command = Commands.literal(Showbiz.MOD_ID)
                .executes { ctx ->
                    ctx.source.sendSuccess({
                        Component.translatable("text.mod.description")
                            .append("\n(You probably intended to use the subcommands)") },
                        false
                    )
                    1
                }
            for (entry in registerCommands(dispatcher,  registry, environment)) {
                command = command.also { it.then(entry) }
            }
            dispatcher.register(command)
        }
    }
}