package com.flooferland.showbiz.commands

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext
import com.flooferland.showbiz.types.permissions.PermissionContext
import com.flooferland.showbiz.utils.MainClientBridge
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component

object ReloadCommand : Command("reload") {
    override fun checkPermission(perms: PermissionContext) = perms.isOp
    override fun description() =
        Component.literal("Reloads both the data pack and resource pack.\n")
            .append("Optionally you can provide the pack ID to make it reload faster")!!

    val pack = args.word("pack")
        .suggests { _, builder ->
            SharedSuggestionProvider.suggest(Showbiz.addonPacks.keys, builder)
        }
        .optional()

    override fun run(ctx: CommandContext): Response {
        val pack = ctx.getArgument(pack)
        if (pack != null) {
            ctx.source.server.reloadResources(listOf(pack))
            MainClientBridge.reloadSpecificPack(pack)
            return Response.success("Reloaded pack '${pack}'")
        } else {
            ctx.source.server.reloadResources(Showbiz.addonPacks.keys)
            MainClientBridge.reloadPacks()
            return Response.success("Reloaded all packs")
        }
    }
}