package com.flooferland.showbiz

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.div

/** NOTE: This should only be called on the server */
object FileStorage {
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
                if (!path.endsWith(".rshw")) continue
                shows.add(path)
            }
        }
        return shows.toTypedArray()
    }
}