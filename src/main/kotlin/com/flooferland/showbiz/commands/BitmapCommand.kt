package com.flooferland.showbiz.commands

import net.minecraft.*
import net.minecraft.commands.*
import net.minecraft.network.chat.*
import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModCommands.bitmapCommandView
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext
import com.flooferland.showbiz.utils.Extensions.hover

object BitmapCommand : Command("bitmap") {
    override val description = Component.literal("Please type in a bitmap name (ex: rae, faz)")!!

    val map = args.word("map")
        .suggests { _, builder ->
            SharedSuggestionProvider.suggest(Showbiz.charts.ids, builder)
        }
    val fixture = args.word("fixture")
        .suggests { ctx, builder ->
            val strings = run {
                val map = ctx.getArgument(map)
                val mapping = BitUtils.readBitmap(map) ?: return@run emptyArray<String>()
                mapping.keys.toTypedArray()
            }
            SharedSuggestionProvider.suggest(strings, builder)
        }
        .optional()

    override fun run(ctx: CommandContext): Response {
        val map = ctx.getArgument(map)
        val mapping = BitUtils.readBitmap(map) ?: return Response.failure("Failed to find bitmap '$map'")

        val fixture = ctx.getArgument(fixture) ?: run {
            val built = Component.literal("Fixtures for the '$map' mapping:\n")
            for ((fixture, _) in mapping) {
                built.append(Component.literal(fixture).withStyle(ChatFormatting.WHITE))
                built.append("\n")
            }
            built.append("Use ${bitmapCommandView(map)} to view the bitmap for a fixture")
            return Response.success(built)
        }

        val movements = mapping[fixture] ?: return Response.failure("Failed to get fixture '$fixture' in map '$map'")
        val built = Component.literal("Bits for '$map/$fixture':\n")
        for ((moveName, moveBit) in movements.entries.sortedWith(compareBy { it.value.toInt() })) {
            val moveComp = Component.literal("- ").withStyle(ChatFormatting.RESET)
            moveComp.append(Component.literal(moveName).withStyle(ChatFormatting.GREEN))
            moveComp.append(Component.literal(": ").withStyle(ChatFormatting.RESET))
            moveComp.append(Component.literal("$moveBit").withStyle(ChatFormatting.AQUA))
            moveComp.append(Component.literal("\n").withStyle(ChatFormatting.DARK_GRAY))
            built.append(moveComp.hover(Drawer.formatBitAsComp(moveBit).append(Component.literal(" (${Drawer.fromBit(moveBit).toStringEnglish()} drawer)").withStyle(
                ChatFormatting.GRAY))))
        }
        return Response.success(built)
    }
}
