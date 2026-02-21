package com.flooferland.showbiz

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import com.flooferland.showbiz.FileStorage.fetchShows
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.network.packets.ShowFileListPacket
import com.flooferland.showbiz.network.packets.ShowFileSelectPacket
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.utils.Extensions.getHeldItem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import kotlin.io.path.name

object FileServer {
    var showWatchService: WatchService =
        FileSystems.getDefault().newWatchService().also {
            FileStorage.SHOWS_DIR.register(it,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
        }

    init {
        ServerPlayNetworking.registerGlobalReceiver(ShowFileListPacket.type) { _, ctx -> sendShowsToClient(ctx.player()) }
        ServerPlayNetworking.registerGlobalReceiver(ShowFileSelectPacket.type) { packet, ctx ->
            val reel = ctx.player().getHeldItem { it.item is ReelItem } ?: return@registerGlobalReceiver
            val filename = packet.selected
            val shows = runCatching { FileStorage.fetchShows() }.onFailure { Showbiz.log.error(it.toString()) }.getOrNull()
            if (shows?.find { it.name == filename } == null) return@registerGlobalReceiver
            reel.applyComponentsAndValidate(
                DataComponentPatch.builder()
                    .set(ModComponents.FileName.type, filename)
                    .build()
            )
        }
    }

    fun update(server: MinecraftServer) {
        val key = showWatchService.poll() ?: return

        var changed = false
        for (event in key.pollEvents()) {
            val path = event.context() as? Path ?: continue
            changed = true
            break
        }
        key.reset()

        if (changed) {
            val files = fetchShows(recache = true).map { it.name }.toTypedArray()
            server.playerList.players.forEach { ServerPlayNetworking.send(it, ShowFileListPacket(toClient = true, files = files)) }
        }
    }

    /** Responds to a client's request to send the file */
    fun sendShowsToClient(player: ServerPlayer) {
        val files = fetchShows().map { it.name }.toTypedArray()
        ServerPlayNetworking.send(player, ShowFileListPacket(toClient = true, files = files))
    }
}