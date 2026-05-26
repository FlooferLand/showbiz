package com.flooferland.showbiz

import java.io.File
import java.nio.file.Path
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import kotlin.io.path.*

/** NOTE: This should only be called on the server */
object FileStorage {
    val SHOWBIZ_DIR = Path(Showbiz.MOD_ID)
    val SHOWS_DIR = SHOWBIZ_DIR / "shows"

    init {
        SHOWS_DIR.toFile().mkdirs()
        ServerLifecycleEvents.SERVER_STARTED.register { _ ->
            cachedShows.clear()
        }
    }

    val cachedShows = hashMapOf<String, Path>()

    private fun addRecursive(dir: File, shows: HashMap<String, Path>) {
        val list = runCatching { dir.listFiles() }
        for (file in list.getOrNull() ?: arrayOf()) {
            if (!file.isDirectory && !Showbiz.charts.extensions.contains(file.extension)) continue
            if (file.isDirectory) {
                addRecursive(file, shows)
            } else {
                val file = file.toPath()
                var chosenName = file.name
                var i = 2
                while (chosenName in shows) {
                    chosenName = "${file.nameWithoutExtension} ($i).${file.extension}"
                    i += 1
                }
                shows[chosenName] = file
            }
        }
    }

    fun fetchShows(recache: Boolean = false): Map<String, Path> {
        if (!recache && cachedShows.isNotEmpty()) return cachedShows

        val shows = hashMapOf<String, Path>()
        runCatching {
            val dir = File(SHOWS_DIR.toString())
            if (!dir.exists()) runCatching { dir.createNewFile() }
            addRecursive(dir, shows)
        }

        cachedShows.clear()
        cachedShows.putAll(shows)
        return cachedShows
    }
}