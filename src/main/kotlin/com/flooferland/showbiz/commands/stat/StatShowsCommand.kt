package com.flooferland.showbiz.commands.stat

import net.minecraft.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.ServerStats
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext
import com.flooferland.showbiz.utils.Extensions.bytesToFriendlySize
import com.flooferland.showbiz.utils.Extensions.hover
import com.flooferland.showbiz.utils.Extensions.withTeleport

object StatShowsCommand : Command("shows") {
    override fun description() = Component.literal("Gives you a list of running shows and whatnot")!!

    val moreInfo = args.bool("more_info").optional()

    override fun run(ctx: CommandContext): Response {
        val list = mutableListOf<Component>()
        val moreInfo = ctx.getArgument(moreInfo) ?: false

        val reelToReels = ServerStats.reelToReels
            .filter { it.showData.name != null }
            .sortedWith(
                compareByDescending<ReelToReelBlockEntity> { it.playing }
                    .thenByDescending { it.showData.approxByteSize() }
                    .thenByDescending { it.showData.video != null }
            )

        var totalByteSize = 0L
        for (entity in reelToReels) {
            val showName = entity.showData.name ?: continue
            val (status, color) =
                if (entity.playing && !entity.paused)
                    "Playing" to ChatFormatting.GREEN
                else if (entity.paused)
                    "Paused" to ChatFormatting.YELLOW
                else
                    "Loaded" to ChatFormatting.GRAY
            val sizeBytes = entity.showData.approxByteSize()
            val size = "~${sizeBytes.bytesToFriendlySize()}"
            val entry = Component.literal(showName)
                .withTeleport(entity.blockPos)
                .hover(
                    Component.literal("Click to teleport to ${entity.blockPos.let { "${it.x} ${it.y} ${it.z}" }}\n")
                        .append(Component.literal("Status: ").append(Component.literal(status).withStyle(color)).append("\n"))
                        .append(Component.literal("Size: ").append(Component.literal(size).withStyle(ChatFormatting.GRAY)).append("\n"))
                )
                .withStyle { it.withColor(color) }
            if (moreInfo)
                entry.append(Component.literal(" ($size)").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
            entry.append(Component.empty().withStyle(ChatFormatting.RESET))
            list += entry
            totalByteSize += sizeBytes
        }

        val comp = Component.empty()
        comp.append(Component.literal("Note that values starting with a tilde are an estimate\n"))
        comp.append(Component.literal("RAM Used: ~${totalByteSize.bytesToFriendlySize()}\n"))
        comp.append(Component.literal("Shows:\n"))
        if (list.isNotEmpty()) {
            list.forEach { comp.append(Component.literal("- ").append(it)).append(Component.literal("\n").withStyle(ChatFormatting.RESET)) }
        } else {
            comp.append(Component.literal("No shows active").withStyle(ChatFormatting.GRAY))
        }
        return Response.success(comp)
    }
}