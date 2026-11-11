package com.flooferland.showbiz.commands

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.addons.AddonNotices
import com.flooferland.showbiz.types.commands.Command
import com.flooferland.showbiz.types.commands.CommandContext
import com.flooferland.showbiz.types.permissions.PermissionContext
import com.flooferland.showbiz.utils.MainClientBridge
import net.minecraft.network.chat.Component

object ReloadCommand : Command("reload") {
    override fun checkPermission(perms: PermissionContext) = perms.isOp
    override fun description() =
        Component.literal("Reloads both the data pack and resource pack.\n")
            .append("Optionally you can provide the pack ID to make it reload faster")!!

    override fun run(ctx: CommandContext): Response {
        val server = ctx.source.server
        val player = ctx.source.player

        player?.sendSystemMessage(Component.literal("Reloading.."))
        AddonNotices.quickReloading = true
        server.packRepository.reload()
        server.reloadResources(server.packRepository.selectedIds).handleAsync { _, throwable ->
            val packs = Showbiz.addonPacks.keys
            MainClientBridge.reloadPacks(packs)
            AddonNotices.quickReloading = false
        }
        return Response.success("Reloaded ${Showbiz.addonPacks.keys.size} pack(s)")
    }
}