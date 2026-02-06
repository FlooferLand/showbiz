package com.flooferland.showbiz.registry

import net.minecraft.ChatFormatting
import net.minecraft.commands.*
import net.minecraft.core.component.*
import net.minecraft.network.chat.*
import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.showbiz.FileStorage
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.show.SignalFrame.Companion.NEXT_DRAWER
import com.flooferland.showbiz.utils.Extensions.asLink
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString

// TODO: FIXME: Clean up the ugliest class in the entire mod (ModCommands)
object ModCommands {
    fun bitmapCommandView(map: String = "<map>", fixture: String = "<fixture>") = "/showbiz bitmap $map $fixture"

    private fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>, registry: CommandBuildContext, environment: Commands.CommandSelection): Array<CommandNode<CommandSourceStack>> {
        val bitmap = Commands.literal("bitmap")
            .executes { ctx ->
                ctx.source.sendFailure(Component.literal("Please type in a bitmap name (ex: rae, faz)"))
                0
            }
            .then(
                Commands.argument("map", StringArgumentType.word())
                    .executes { ctx ->
                        val mapName = StringArgumentType.getString(ctx, "map") ?: return@executes 1
                        val mapping = BitUtils.readBitmap(mapName)
                        if (mapping != null) {
                            val built = Component.literal("Fixtures for the '$mapName' mapping:\n")
                            for ((fixture, _) in mapping) {
                                built.append(Component.literal(fixture).withStyle(ChatFormatting.WHITE))
                                built.append("\n")
                            }
                            built.append("Use ${bitmapCommandView(map = mapName)} to view the bitmap for a fixture")
                            ctx.source.sendSuccess({ built }, true)
                            0
                        } else {
                            ctx.source.sendFailure(Component.literal("Mapping '$mapName' does not exist. Try 'rae' or 'faz'."))
                            1
                        }
                    }
                    .then (
                        Commands.argument("fixture", StringArgumentType.word())
                            .executes { ctx ->
                                val mapName = StringArgumentType.getString(ctx, "map") ?: return@executes 1
                                val fixtureName = StringArgumentType.getString(ctx, "fixture") ?: return@executes 1
                                val mapping = BitUtils.readBitmap(mapName) ?: return@executes 1

                                val movements = mapping[fixtureName]
                                if (movements != null) {
                                    val built = Component.literal("Bits for '$mapName/$fixtureName':\n")
                                    for ((moveName, moveBit) in movements) {
                                        val moveComp = Component.literal("- ").withStyle(ChatFormatting.RESET)
                                        moveComp.append(Component.literal(moveName).withStyle(ChatFormatting.DARK_GREEN))
                                        moveComp.append(Component.literal(": ").withStyle(ChatFormatting.RESET))
                                        moveComp.append(Component.literal("$moveBit ").withStyle(ChatFormatting.BLUE))
                                        moveComp.append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY))
                                        moveComp.append(Component.literal((if (moveBit > NEXT_DRAWER) moveBit - NEXT_DRAWER else moveBit).toString() + " ").withStyle(ChatFormatting.DARK_GRAY))
                                        moveComp.append(Drawer.fromBit(moveBit).toCompDrawer().withStyle(ChatFormatting.DARK_GRAY))
                                        moveComp.append(Component.literal(")\n").withStyle(ChatFormatting.DARK_GRAY))
                                        built.append(moveComp)
                                    }
                                    ctx.source.sendSuccess({ built }, true)
                                    0
                                } else {
                                    ctx.source.sendFailure(Component.literal("Fixture '${fixtureName}' wasn't found."))
                                    1
                                }
                            }
                    )
            )
            .build()

        val reelAdd = Commands.literal("reelupload")
            .executes { ctx ->
                val shows = runCatching { FileStorage.fetchShows() }.onFailure { Showbiz.log.error(it.toString()) }.getOrNull()
                if (shows.isNullOrEmpty()) {
                    ctx.source.sendFailure(Component.literal("No shows found.\nUpload an ${FileStorage.SUPPORTED_FORMATS.joinToString("/")} show file to your ${FileStorage.SHOWS_DIR.pathString}"))
                    return@executes 0
                }

                val showsComp = Component.empty().also { showsComp ->
                    shows.map { path ->
                        val hover = Component.literal("Click to upload to a reel you're holding\n")
                            .append(Component.literal(path.name).withStyle(ChatFormatting.GRAY))
                        Component.literal("${path.nameWithoutExtension}\n")
                            .withStyle { it
                                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                                .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/$MOD_ID reelupload ${path.name}"))
                                .withBold(true)
                            }
                    }.forEach { showsComp.append(Component.literal("- ").append(it)) }
                }
                ctx.source.sendSuccess(
                    { Component.literal("Click or run the command with one of the following file names:\n").append(showsComp) },
                    false
                )
                0
            }
            .then(
                Commands.argument("file", StringArgumentType.greedyString())
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
                            return@executes err("File name must end with the supported file extensions: [${FileStorage.SUPPORTED_FORMATS.joinToString(", ") }]")
                        }

                        val shows = runCatching { FileStorage.fetchShows() }.onFailure { Showbiz.log.error(it.toString()) }.getOrNull()
                        if (shows?.find { it.name == filename } == null) {
                            ctx.source.sendFailure(
                                Component.literal("File does not exist: '${filename}'\n")
                                    .append(Component.literal("  Hint: Run the command with no file to list all files\n"))
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

        val clientWiki = Commands.literal("wiki")
            .executes { ctx ->
                ctx.source.sendSuccess({ Component.literal("https://github.com/FlooferLand/showbiz/wiki").asLink() }, true)
                0
            }
            .build()

        return arrayOf(reelAdd, bitmap, clientWiki)
    }

    init {
        CommandRegistrationCallback.EVENT.register { dispatcher, registry, environment ->
            var command = Commands.literal(MOD_ID)
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