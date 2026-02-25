package com.flooferland.showbiz

import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div

/** NOTE: This should only be called on the server */
object FileStorage {
    val SUPPORTED_FORMATS = listOf("rshw", "fshw")
    val SHOWBIZ_DIR = Path(Showbiz.MOD_ID)
    val SHOWS_DIR = SHOWBIZ_DIR / "shows"

    private var cachedShows = arrayOf<Path>()

    fun fetchShows(recache: Boolean = false): Array<Path> {
        val recache = recache || cachedShows.isEmpty()
        if (recache) {
            val shows = mutableListOf<Path>()
            runCatching {
                val dir = File(SHOWS_DIR.toString())
                if (!dir.exists()) runCatching { dir.createNewFile() }
                val list = runCatching { dir.listFiles() }
                for (file in list.getOrNull() ?: arrayOf()) {
                    if (!SUPPORTED_FORMATS.contains(file.extension)) continue
                    shows.add(file.toPath())
                }
            }
            cachedShows = shows.toTypedArray()
        }
        return cachedShows
    }
}