package com.flooferland.showbiz.types

import com.flooferland.showbiz.Showbiz
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
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

        // TODO: Make sure FFmpeg can't block or pause the thread
        fun nextFrame(): ByteArray? {
            if (!process.isAlive) return null
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

    val videoExtensions = setOf("mp4", "webm", "mkv", "avi", "mov")

    private var mainFile: File? = null
    private var probeFile: File? = null
    private var error: String? = null

    val localAvailable get() = mainFile?.exists() == true
    var serverAvailable = false

    init { init() }

    fun init() {
        if (mainFile != null && probeFile != null) return
        mainFile = findFile("ffmpeg") ?: run { setError("Failed to find FFmpeg executable"); return@run null }
        probeFile = findFile("ffprobe") ?: run { setError("Failed to find FFprobe executable"); return@run null }
        Showbiz.log.info("FFmpeg: '${mainFile?.absolutePath}'")
        Showbiz.log.info("FFprobe: '${probeFile?.absolutePath}'")
    }

    private fun findFile(name: String): File? = runCatching {
        val isWindows = System.getProperty("os.name").contains("win", ignoreCase = true)
        val binaryName = if (isWindows) "$name.exe" else name

        val pathEnv = System.getenv("PATH") ?: return null
        val pathSep = if (isWindows) ";" else ":"

        var foundPath = pathEnv.split(pathSep)
            .asSequence()
            .filter { it.isNotEmpty() }
            .map { File(it.replace("\"", ""), binaryName) }
            .firstOrNull { it.isFile && it.canExecute() }
        if (foundPath == null || !foundPath.exists()) {
            Showbiz.log.warn("$name wasn't found in the path.. Searching elsewhere")
            if (isWindows) {
                val process = startProcess("powershell", "-c", "(Get-Command $name).Source")
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor(3, TimeUnit.SECONDS)
                if (output.isNotEmpty()) {
                    Showbiz.log.info("$name found through PowerShell (output='$output')")
                    foundPath = Paths.get(output).toFile()
                }
            }
        }
        if (foundPath == null || !foundPath.exists()) {
            Showbiz.log.info("$name wasn't found anywhere")
            return null
        }

        val version = runCatching {
            startProcess(name, "-version").inputStream.bufferedReader().readText()
                .trim().split(' ').subList(0, 3).joinToString(" ")
        }.getOrNull()
        Showbiz.log.info("$name: $version")
        foundPath
    }.also {
        it.onFailure { throwable -> Showbiz.log.error("Unknown exception triggered while trying to find '$name': ", throwable) }
    }.getOrNull()

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
            val process = startProcess(
                ffmpeg, "-y",
                "-i", tempIn.absolutePath,
                "-acodec", settings.audio.codec,
                "-ac", settings.audio.channels.toString(),
                "-ar", settings.audio.sampleRate.toString(),
                tempOut.absolutePath
            )

            val exitCode = process.onExit().await().exitValue()
            if (exitCode != 0) {
                val errorLog = process.errorStream.bufferedReader().readText()
                setError("Encode failed (exitCode=${exitCode}):\n${errorLog}")
                return@withContext null
            }
            return@withContext tempOut.readBytes()
        } catch (e: Exception) {
            setError("Failed during encode: $e")
            null
        } finally {
            tempIn.delete()
            tempOut.delete()
        }
    }

    /** Gets the video size and other stuff */
    suspend fun probeVideo(path: Path): VideoInfo? = withContext(Dispatchers.IO) {
        val ffprobe = probeFile?.absolutePath ?: return@withContext null
        val process = startProcess(
            ffprobe,
            "-v", "quiet",
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height,r_frame_rate",
            "-of", "csv=p=0",
            path.absolutePathString()
        )
        val output = process.inputStream.bufferedReader().readText().trim().let { it.lines().firstOrNull() ?: it }
        process.waitFor()

        val parts = output.split(",")
        if (parts.size < 3) return@withContext null

        val (num, den) = parts[2].split("/").map { it.trim().toDouble() }
        VideoInfo(path, parts[0].toInt(), parts[1].toInt(), num / den)
    }

    fun openVideoStream(input: VideoInfo, output: VideoInfo = input, seek: Duration): VideoStream? {
        val ffmpeg = mainFile?.absolutePath ?: run { setError("FFmpeg not found"); return null }
        val process = startProcess(
            ffmpeg,
            "-ss", seek.toDouble(DurationUnit.SECONDS).toString(),
            "-i", input.path.absolutePathString(),
            "-f", "rawvideo",
            "-pix_fmt", "rgb24",
            "-r", output.fps.toString(),
            "-vf", "scale=${output.width}:${output.height}",
            "-an",
            "pipe:1"
        )
        return VideoStream(process, input, output)
    }

    private fun setError(text: String) {
        error = text
        Showbiz.log.error("FFmpeg error: $text")
    }

    private fun startProcess(vararg args: String): Process {
        val process = ProcessBuilder(*args)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        if (Showbiz.log.isDebugEnabled) {
            Showbiz.log.debug("FFmpeg ${process.pid()} - ${args.joinToString(" ")}")
        }
        return process
    }
}