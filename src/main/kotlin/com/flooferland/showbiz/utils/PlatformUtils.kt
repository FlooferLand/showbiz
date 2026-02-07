package com.flooferland.showbiz.utils

import java.nio.file.Path

object PlatformUtils {
    fun openFileManager(path: Path) {
        val dir = path.toFile().also { runCatching { it.mkdirs() } }
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        when {
            os.contains("win") -> {
                ProcessBuilder("explorer.exe", dir.absolutePath).start()
            }
            os.contains("mac") || os.contains("osx") -> {
                ProcessBuilder("open", dir.absolutePath).start()
            }
            else -> {
                ProcessBuilder("xdg-open", dir.absolutePath).start()
            }
        }
    }
}