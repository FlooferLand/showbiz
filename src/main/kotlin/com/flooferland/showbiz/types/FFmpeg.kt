package com.flooferland.showbiz.types

import com.flooferland.showbiz.Showbiz
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.DurationUnit

// TODO: Figure out a way to network FFmpeg (ex: If the client has it, encode/decode on the client. If only the server has it, encode/decode on the server)

/** Other FFmpeg libraries like Jave are sloppily thrown together, so a manual search / process call for it is better */
object FFmpeg {
    data class AudioSettings(val codec: String, val channels: Int, val sampleRate: Int)
    data class Settings(val outputFormat: String, val audio: AudioSettings)
    data class VideoInfo(val path: Path, val width: Int, val height: Int, val fps: Double)
    class VideoStream(private var process: Process, val input: VideoInfo, val output: VideoInfo) : AutoCloseable {
        private val stdout = process.inputStream
        val colorChannels = 3
        val frameSize = output.width * output.height * colorChannels

        fun nextFrame(): ByteArray? {
            val buffer = ByteArray(frameSize)
            var offset = 0
            while (offset < frameSize) {
                val read = stdout.read(buffer, offset, frameSize - offset)
                if (read == -1) return null
                offset += read
            }
            return buffer
        }

        /** Creates a new process because FFmpeg doesn't seem to support seeking */
        fun seek(seek: Duration) {
            FFmpeg.openVideoStream(input, output, seek)?.let {
                process.destroy()
                process = it.process
            }
        }
        override fun close() = process.destroy()
    }

    private var mainFile: File? = null
    private var probeFile: File? = null
    private var error: String? = null

    val localAvailable get() = mainFile?.exists() == true
    var serverAvailable = false

    init { init() }

    fun init() {
        if (mainFile != null) return
        mainFile = findFile("ffmpeg") ?: run { setError("Failed to find FFmpeg executable"); return }
        probeFile = findFile("ffprobe") ?: run { setError("Failed to find FFprobe executable"); return }
        Showbiz.log.info("FFmpeg found at '${mainFile?.absolutePath}'")
    }

    fun getLastError() = error

    suspend fun encode(inputBytes: ByteArray, settings: Settings): ByteArray? = withContext(Dispatchers.IO) {
        val ffmpeg = mainFile?.absolutePath ?: run {
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

    suspend fun probeVideo(path: Path): VideoInfo? = withContext(Dispatchers.IO) {
        val ffprobe = probeFile?.absolutePath ?: return@withContext null
        val process = ProcessBuilder(
            ffprobe,
            "-v", "quiet",
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height,r_frame_rate",
            "-of", "csv=p=0",
            path.absolutePathString()
        ).redirectError(ProcessBuilder.Redirect.DISCARD).start()

        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()

        val parts = output.split(",")
        if (parts.size < 3) return@withContext null

        val (num, den) = parts[2].split("/").map { it.toDouble() }
        VideoInfo(path, parts[0].toInt(), parts[1].toInt(), num / den)
    }

    fun openVideoStream(input: VideoInfo, output: VideoInfo = input, seek: Duration): VideoStream? {
        val ffmpeg = mainFile?.absolutePath ?: run { setError("FFmpeg not found"); return null }
        val process = ProcessBuilder(
            ffmpeg,
            "-ss", seek.toDouble(DurationUnit.SECONDS).toString(),
            "-i", input.path.absolutePathString(),
            "-f", "rawvideo",
            "-pix_fmt", "rgb24",
            "-r", output.fps.toString(),
            "-vf", "scale=${output.width}:${output.height}",
            "-an",
            "pipe:1"
        ).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        return VideoStream(process, input, output)
    }

    private fun setError(text: String) {
        error = text
        Showbiz.log.error("FFmpeg error: $text")
    }

    private fun findFile(name: String): File? {
        val isWindows = System.getProperty("os.name").contains("win", ignoreCase = true)
        val binaryName = if (isWindows) "$name.exe" else name

        val pathEnv = System.getenv("PATH") ?: return null
        val pathSep = if (isWindows) ";" else ":"

        return pathEnv.split(pathSep)
            .asSequence()
            .filter { it.isNotEmpty() }
            .map { Paths.get(it).resolve(binaryName) }
            .firstOrNull { Files.isRegularFile(it) && Files.isExecutable(it) }
            ?.toFile()
    }
}