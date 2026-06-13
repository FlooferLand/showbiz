package com.flooferland.showbiz

import net.minecraft.*
import net.minecraft.core.component.*
import net.minecraft.network.chat.*
import net.minecraft.server.*
import net.minecraft.server.level.*
import com.flooferland.bizlib.RawShowData
import com.flooferland.bizlib.formats.RshowFormat
import com.flooferland.showbiz.FileStorage.fetchShows
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.network.packets.*
import com.flooferland.showbiz.network.packets.FileUploadResponsePacket.ServerMessage
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.types.FFmpeg
import com.flooferland.showbiz.types.FFmpeg.AudioSettings
import com.flooferland.showbiz.types.FFmpeg.Settings
import com.flooferland.showbiz.types.ServerPlayerFileUpload
import com.flooferland.showbiz.types.WavHeader
import com.flooferland.showbiz.utils.Extensions.getHeldItem
import java.nio.file.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import kotlin.io.path.extension

object FileServer {
    var serverPlayerUploads = mutableMapOf<Int, ServerPlayerFileUpload>()
    var showWatchService: WatchService =
        FileSystems.getDefault().newWatchService().also {
            FileStorage.SHOWS_DIR.register(it,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
        }

    init {
        // Show files
        ServerPackets.listen(ShowFileListPacket.type) { _, ctx -> sendShowsToClient(ctx.player()) }
        ServerPackets.listen(ShowFileSelectPacket.type) { packet, ctx ->
            val reel = ctx.player().getHeldItem { it.item is ReelItem } ?: return@listen
            val filename = packet.selected
            val shows = runCatching { FileStorage.fetchShows() }.onFailure { Showbiz.log.error(it.toString()) }.getOrNull()
            if (shows?.containsKey(filename) != true) {
                serverError("Show file '$filename' does not exist", ctx.player())
                return@listen
            }
            reel.applyComponentsAndValidate(
                DataComponentPatch.builder()
                    .set(ModComponents.FileName.type, filename)
                    .build()
            )
        }
        ServerPackets.listen(ShowFileEditPacket.type) { packet, ctx ->
            if (!Permissions.canWriteReels(ctx.player())) {
                ctx.player().sendSystemMessage(Component.literal("You don't have the permission to write files"))
                return@listen
            }
            var path = FileStorage.SHOWS_DIR.resolve(packet.file)
            val player = ctx.player()
            val server = ctx.server()
            if (path.extension.isBlank()) {
                serverError("Failed to make a show. Missing file extension ('${path}')", player)
                return@listen
            }

            when (packet.action) {
                FileAction.Create -> CoroutineScope(Dispatchers.IO).launch {
                    val audioBytes = serverPlayerUploads.remove(player.id)?.bytes.orEmpty().toByteArray()
                    val result = runCatching {
                        val settings = Settings(outputFormat = "wav", audio = AudioSettings(codec = "pcm_s16le", channels = 2, sampleRate = 44100))
                        val audioWav = when {
                            WavHeader.isWav(audioBytes) -> audioBytes
                            FFmpeg.localAvailable -> FFmpeg.encode(audioBytes, settings) ?: error("Failed to encode audio bytes to wav: ${FFmpeg.getLastError()}")
                            else -> error("Failed to create a show file. Audio is not using the WAV format. Please install FFMPEG if you want to use other audio formats")
                        }
                        Files.newOutputStream(path).use {
                            RshowFormat().write(it, RawShowData(audio = audioWav))
                        }
                    }
                    server.execute {
                        if (!player.connection.isAcceptingMessages) return@execute
                        result.onSuccess {
                            player.getHeldItem { it.item is ReelItem }?.let { ReelItem.setFilename(it, packet.file) }
                            sendShowsToClient(player)
                        }.onFailure { throwable ->
                            serverError(throwable, player)
                            sendShowsToClient(player)
                        }
                    }
                }

                FileAction.Delete -> runCatching {
                    Files.deleteIfExists(path)
                    sendShowsToClient(player)
                }.onFailure { serverError(it, player) }
            }
        }

        // File uploads
        ServerPackets.listen(FileUploadHeaderPacket.type) { headerPacket, ctx ->
            val player = ctx.player()
            val upload = ServerPlayerFileUpload(headerPacket.file, headerPacket.fileSizeBytes)
            serverPlayerUploads[player.id] = upload
            ServerPlayNetworking.send(player, FileUploadResponsePacket(ServerMessage.Continue, bytesSoFar = upload.getSizeBytes()))
        }
        ServerPackets.listen(FileUploadChunkPacket.type) { packet, ctx ->
            val player = ctx.player()
            val upload = serverPlayerUploads[player.id]
            if (upload != null && packet.chunk.isNotEmpty()) upload.bytes.addAll(packet.chunk.toTypedArray())
            val done = (upload?.let { it.getSizeBytes() >= it.maxSizeBytes } ?: true) || packet.chunk.isEmpty()
            val message = if (upload == null) ServerMessage.BuzzOff else if (done) ServerMessage.Done else ServerMessage.Continue
            ServerPlayNetworking.send(player, FileUploadResponsePacket(message, bytesSoFar = upload?.getSizeBytes() ?: 0L))
        }
    }

    fun serverError(text: String, player: ServerPlayer? = null) {
        Showbiz.log.error("SERVER ERROR: $text")
        player?.sendSystemMessage(Component.literal("Server error: $text").withStyle(ChatFormatting.RED))
    }
    fun serverError(throwable: Throwable, player: ServerPlayer? = null) {
        Showbiz.log.error("SERVER ERROR:", throwable)
        player?.sendSystemMessage(Component.literal("Server error: $throwable").withStyle(ChatFormatting.RED))
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
            val ids = fetchShows(recache = true).keys.sorted().toCollection(LinkedHashSet())
            server.playerList.players.forEach {
                ServerPlayNetworking.send(it, ShowFileListPacket(toClient = true, fileIds = ids, playerAuthorized = Permissions.canWriteReels(it)))
            }
        }
    }

    /** Responds to a client's request to send the file */
    fun sendShowsToClient(player: ServerPlayer) {
        val ids = fetchShows().keys.sorted().toCollection(LinkedHashSet())
        val authorized = Permissions.canWriteReels(player)
        ServerPlayNetworking.send(player, ShowFileListPacket(toClient = true, fileIds = ids, playerAuthorized = authorized))
    }

    enum class FileAction {
        Create, Delete
    }
}