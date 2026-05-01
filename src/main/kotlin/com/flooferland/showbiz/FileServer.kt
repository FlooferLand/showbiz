package com.flooferland.showbiz

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import com.flooferland.bizlib.RawShowData
import com.flooferland.bizlib.formats.RshowFormat
import com.flooferland.showbiz.FileStorage.fetchShows
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.network.packets.ShowFileEditPacket
import com.flooferland.showbiz.network.packets.ShowFileListPacket
import com.flooferland.showbiz.network.packets.ShowFileSelectPacket
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.utils.Extensions.getHeldItem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.security.Permission
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.name
import kotlin.io.path.writeBytes

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
        ServerPlayNetworking.registerGlobalReceiver(ShowFileEditPacket.type) { packet, ctx ->
            if (!Permissions.canWriteReels(ctx.player())) {
                ctx.player().sendSystemMessage(Component.literal("You don't have the permission to write files"))
                return@registerGlobalReceiver
            }
            val path = FileStorage.SHOWS_DIR.resolve(packet.file)
            when (packet.action) {
                FileAction.Create -> runCatching {
                    val stream = Files.newOutputStream(path)
                    RshowFormat().write(stream, RawShowData())
                }
                FileAction.Delete -> runCatching {
                    Files.deleteIfExists(path)
                }
                else -> Showbiz.log.error("File action '${packet.action}' is unsupported")
            }
            sendShowsToClient(ctx.player())
        }
    }

    fun update(server: MinecraftServer) {
        val key = showWatchService.poll() ?: return

        var changed = false
        for (event in key.pollEvents()) {
            val path = event.context() as? Path ?: continue
            Showbiz.log.debug("Show file changed: {}", path)
            changed = true
            break
        }
        key.reset()

        if (changed) {
            val files = fetchShows(recache = true).map { it.name }.toTypedArray()
            server.playerList.players.forEach {
                ServerPlayNetworking.send(it, ShowFileListPacket(toClient = true, files = files, playerAuthorized = Permissions.canWriteReels(it)))
            }
        }
    }

    /** Responds to a client's request to send the file */
    fun sendShowsToClient(player: ServerPlayer) {
        val files = fetchShows().map { it.name }.toTypedArray()
        val authorized = Permissions.canWriteReels(player)
        ServerPlayNetworking.send(player, ShowFileListPacket(toClient = true, files = files, playerAuthorized = authorized))
    }

    enum class FileAction {
        Create, Delete
    }
}