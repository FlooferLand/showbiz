package com.flooferland.showbiz.types

import com.flooferland.showbiz.Showbiz
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

// TODO: Figure out a way to network FFmpeg (ex: If the client has it, encode/decode on the client. If only the server has it, encode/decode on the server)

/** Other FFmpeg libraries like Jave are sloppily thrown together, so a manual search / process call for it is better */
object FFmpeg {
    data class AudioSettings(val codec: String, val channels: Int, val sampleRate: Int)
    data class Settings(val outputFormat: String, val audio: AudioSettings)

    private var file: File? = null
    private var error: String? = null

    val localAvailable get() = file?.exists() == true
    var serverAvailable = false

    fun init() {
        val ffmpeg = findFile() ?: run { setError("Failed to find executable"); return }
        Showbiz.log.debug("FFmpeg found at '${ffmpeg.absolutePath}'")
    }

    fun getLastError() = error

    suspend fun encode(inputBytes: ByteArray, settings: Settings): ByteArray? = withContext(Dispatchers.IO) {
        val ffmpeg = file?.absolutePath ?: run {
            setError("Executable was not found during encode")
            return@withContext null
        }

        val tempIn = Files.createTempFile("showbiz_in_", ".tmp").toFile()
        val tempOut = Files.createTempFile("showbiz_out_", ".wav").toFile()
        try {
            tempIn.writeBytes(inputBytes)
            val process = ProcessBuilder(
                ffmpeg, "-y",
                "-i", tempIn.absolutePath,
                "-acodec", settings.audio.codec,
                "-ac", settings.audio.channels.toString(),
                "-ar", settings.audio.sampleRate.toString(),
                tempOut.absolutePath
            ).start()

            val exitCode = process.onExit().await().exitValue()
            if (exitCode != 0) {
                val errorLog = process.errorStream.bufferedReader().readText()
                setError("Encode failed (exitCode=${exitCode}):\n${errorLog}")
                return@withContext null
            }

            tempOut.readBytes()
        } catch (e: Exception) {
            setError("Failed during encode: $e")
            null
        } finally {
            tempIn.delete()
            tempOut.delete()
        }
    }

    private fun setError(text: String) {
        error = text
        Showbiz.log.error("FFmpeg error: $text")
    }

    private fun findFile(): File? {
        val isWindows = System.getProperty("os.name").contains("win", ignoreCase = true)
        val binaryName = if (isWindows) "ffmpeg.exe" else "ffmpeg"

        val pathEnv = System.getenv("PATH") ?: return null
        val pathSep = if (isWindows) ";" else ":"

        file = pathEnv.split(pathSep)
            .asSequence()
            .filter { it.isNotEmpty() }
            .map { Paths.get(it).resolve(binaryName) }
            .firstOrNull { Files.isRegularFile(it) && Files.isExecutable(it) }
            ?.toFile()
        return file
    }

    init { init() }
}