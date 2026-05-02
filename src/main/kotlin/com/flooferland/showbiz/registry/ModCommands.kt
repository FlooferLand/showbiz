package com.flooferland.showbiz.registry

import net.minecraft.*
import net.minecraft.commands.*
import net.minecraft.core.component.*
import net.minecraft.network.chat.*
import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.showbiz.FileStorage
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.utils.Extensions.asLink
import com.flooferland.showbiz.utils.Extensions.hover
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.CommandNode
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import kotlin.io.path.*

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
                    .suggests { _, builder -> SharedSuggestionProvider.suggest(Showbiz.charts.ids, builder) }
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
                            ctx.source.sendFailure(Component.literal("Mapping '$mapName' does not exist."))
                            1
                        }
                    }
                    .then (
                        Commands.argument("fixture", StringArgumentType.word())
                            .suggests { ctx, builder ->
                                val strings = run {
                                    val mapName = StringArgumentType.getString(ctx, "map") ?: return@run emptyArray<String>()
                                    val mapping = BitUtils.readBitmap(mapName) ?: return@run emptyArray<String>()
                                    mapping.keys.toTypedArray()
                                }
                                SharedSuggestionProvider.suggest(strings, builder)
                            }
                            .executes { ctx ->
                                val mapName = StringArgumentType.getString(ctx, "map") ?: return@executes 1
                                val fixtureName = StringArgumentType.getString(ctx, "fixture") ?: return@executes 1
                                val mapping = BitUtils.readBitmap(mapName) ?: return@executes 1

                                val movements = mapping[fixtureName]
                                if (movements != null) {
                                    val built = Component.literal("Bits for '$mapName/$fixtureName':\n")
                                    for ((moveName, moveBit) in movements.entries.sortedWith(compareBy { it.value.toInt() })) {
                                        val moveComp = Component.literal("- ").withStyle(ChatFormatting.RESET)
                                        moveComp.append(Component.literal(moveName).withStyle(ChatFormatting.GREEN))
                                        moveComp.append(Component.literal(": ").withStyle(ChatFormatting.RESET))
                                        moveComp.append(Component.literal("$moveBit").withStyle(ChatFormatting.AQUA))
                                        moveComp.append(Component.literal("\n").withStyle(ChatFormatting.DARK_GRAY))
                                        built.append(moveComp.hover(Drawer.formatBitAsComp(moveBit).append(Component.literal(" (${Drawer.fromBit(moveBit).toStringEnglish()} drawer)").withStyle(
                                            ChatFormatting.GRAY))))
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

        val clientWiki = Commands.literal("wiki")
            .executes { ctx ->
                ctx.source.sendSuccess({ Component.literal("https://github.com/FlooferLand/showbiz/wiki").asLink() }, true)
                0
            }
            .build()

        return arrayOf(bitmap, clientWiki)
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