package com.flooferland.showbiz

import com.flooferland.showbiz.types.FFmpeg
import com.flooferland.showbiz.types.ShowFileInfo
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/** NOTE: This should only be called on the server */
object FileStorage {
    val SHOWBIZ_DIR = Path(Showbiz.MOD_ID)
    val SHOWS_DIR = SHOWBIZ_DIR / "shows"

    init {
        runCatching { Files.createDirectories(SHOWS_DIR) }
        ServerLifecycleEvents.SERVER_STARTED.register { _ ->
            cachedShowPaths.clear()
            cachedShowInfo.clear()
        }
    }

    private val cachedShowPaths = hashMapOf<String, Path>()
    private val cachedShowInfo = hashMapOf<String, ShowFileInfo>()

    fun getShowVideo(showPath: Path): Path? {
        var videoPath = runCatching {
            showPath.parent.listDirectoryEntries()
                .firstOrNull { it.nameWithoutExtension == showPath.nameWithoutExtension && it.extension.lowercase() in FFmpeg.videoExtensions }
        }.getOrNull()
        if (videoPath == null)
            videoPath = showPath.parent / Path(showPath.nameWithoutExtension + ".mp4")
        return if (Files.exists(videoPath)) videoPath else null
    }

    fun fetchShowInfos(recache: Boolean = false) = fetchShowPaths(recache).map { (id, path) ->
        val videoPath = getShowVideo(path)
        ShowFileInfo(id, videoPath != null)
    }.sortedBy { it.id }

    @Suppress("SameReturnValue")
    fun fetchShowPaths(recache: Boolean = false): Map<String, Path> {
        if (!recache && cachedShowPaths.isNotEmpty()) return cachedShowPaths

        val shows = hashMapOf<String, Path>()
        runCatching {
            val dir = SHOWS_DIR.toFile()
            if (!dir.exists()) runCatching { dir.createNewFile() }
            addShowsRecursive(dir, shows)
        }

        cachedShowPaths.clear()
        cachedShowPaths.putAll(shows)
        return cachedShowPaths
    }

    private fun addShowsRecursive(dir: File, shows: HashMap<String, Path>) {
        val list = runCatching { dir.listFiles() }
        for (file in list.getOrNull() ?: arrayOf()) {
            val extension = file.extension
            val validExtension = Showbiz.charts.extensions.contains(extension)
                    || (extension.endsWith("shw") && extension != "shw")
            if (!file.isDirectory && !validExtension) continue

            if (file.isDirectory) {
                addShowsRecursive(file, shows)
            } else {
                val file = file.toPath()
                var chosenName = file.name
                var i = 2
                while (chosenName in shows) {
                    chosenName = "${file.nameWithoutExtension} ($i).${extension}"
                    i += 1
                }
                shows[chosenName] = file
            }
        }
    }
}