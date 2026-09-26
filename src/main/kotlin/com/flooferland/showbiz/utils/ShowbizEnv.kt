package com.flooferland.showbiz.utils

import com.flooferland.showbiz.Showbiz
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader

/** Environment/system utilities */
object ShowbizEnv {
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
    fun isDev() =
        FabricLoader.getInstance()?.isDevelopmentEnvironment == true
    fun devThrow(text: String) = if (isDev()) error(text) else Showbiz.log.error(text)
    fun <T> assert(a: T, b: T, ctx: String) where T: Comparable<T> {
        if (a != b) devThrow("Assertion failed: $a != $b ($ctx)")
    }
}