package com.flooferland.showbiz

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.extension

/** NOTE: This should only be called on the server */
object FileStorage {
    val SUPPORTED_FORMATS = listOf("rshw", "fshw")
    val SHOWBIZ_DIR = Path(Showbiz.MOD_ID)
    val SHOWS_DIR = SHOWBIZ_DIR / "shows"

    // TODO: Should probably cache this
    fun fetchShows(): Array<String> {
        val shows = mutableListOf<String>()
        runCatching {
            val dir = File(SHOWS_DIR.toString())
            if (!dir.exists()) runCatching { dir.createNewFile() }
            val list = runCatching { dir.list() }
            for (path in list.getOrNull() ?: arrayOf()) {
                if (!SUPPORTED_FORMATS.contains(Path(path).extension)) continue
                shows.add(path)
            }
        }
        return shows.toTypedArray()
    }
}