package com.flooferland.showbiz.commands

import com.flooferland.showbiz.network.packets.OpenClientConfigPacket
import com.flooferland.showbiz.registry.ModConfig
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component

object ConfigCommand : Command("config") {
    override fun description() = Component.literal("Opens up the client-side config screen")!!
    override fun run(ctx: CommandContext): Response {
        val comp = usage().copy().append("\n")
        val player = ctx.source.player
        if (player == null) {
            comp.append(
                Component.literal(
                    "- If you want to configure the server, visit '${
                        FabricLoader.getInstance().gameDir.relativize(
                            ModConfig.getCommonPath()
                        )
                    }'\n"
                )
            )
        } else {
            ServerPlayNetworking.send(player, OpenClientConfigPacket())
            comp.append(Component.literal("- If a config screen didn't open just now, install Mod Menu and configure the mod there\n"))
        }
        return Response.success(comp)
    }
}