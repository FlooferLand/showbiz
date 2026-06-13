package com.flooferland.showbiz.show

import net.minecraft.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.world.entity.player.*
import com.flooferland.bizlib.RawShowData
import com.flooferland.bizlib.formats.LegacyRshowFormat
import com.flooferland.bizlib.formats.RshowFormat
import com.flooferland.showbiz.FileStorage
import com.flooferland.showbiz.FileStorage.getShowVideo
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.types.FFmpeg
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import com.flooferland.showbiz.utils.Extensions.getUUIDOrNull
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import com.flooferland.showbiz.utils.Sounds
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.UUID
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlinx.coroutines.*
import kotlin.io.path.Path
import kotlin.time.Duration

// TODO: Ensure the file is saved correctly,
//       even when the user spams the record button and does whatever weird things users do

// TODO: Optimize this! Right now when loading/unloading shows, signal data is stored twice!

/**
 * Abstraction class to work with rshw.
 * Every rshw frame is a list of currently played bits, collecting that at load time is way nicer.
 */
class ShowData(val owner: ReelToReelBlockEntity) {
    // TODO: Convert signal to a list of longs and pack bit ids using bitwise operations
    val signal: MutableList<BitIdArray> = ArrayList()
    var audio: ByteArray = ByteArray(0)
    var videoInputInfo: FFmpeg.VideoInfo? = null
    var video: FFmpeg.VideoStream? = null
    var id: UUID? = null
    var name: String? = null
    var format: AudioFormat? = null
    var mapping: String? = null
    var loadJob: Job? = null

    val targetFormat = AudioFormat(
        44100f,
        16,
        1,
        true,
        false
    )

    var loading = false
    var isLoaded = false

    fun getFilePath(filename: String) = FileStorage.cachedShowPaths[filename] ?: Path("${FileStorage.SHOWS_DIR}/$filename")
    fun isEmpty() = !isLoaded

    fun load(filename: String, onLoadOrErr: (data: ShowData?, error: Component?) -> Unit = { _, _ -> }) {
        unload()
        name = filename
        loading = true
        id = UUID.randomUUID()

        Showbiz.log.debug("Loading tape '${filename}' ($mapping)")

        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->  }
        loadJob = CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            mapping = run {
                val ext = if (filename.contains('.')) filename.substringAfterLast('.') else run {
                    error("The file is missing a file extension")
                }
                Showbiz.charts.extensionToId[ext] ?: run {
                    error("Format '${ext}' was not among: [${Showbiz.charts.extensions.joinToString()}]")
                }
            }
            val path = getFilePath(filename)
            val loaded = run {
                val out = runCatching { Files.newInputStream(path).use { RshowFormat().read(it) } }
                out.onFailure { throwable ->
                    Showbiz.log.error("BizlibNative failed to load '${path}'. Using fallback Java-based reader", throwable)
                }
                out.getOrNull() ?: LegacyRshowFormat().readFile(path)
            }

            // Audio and conversion
            run {
                val source = AudioSystem.getAudioInputStream(ByteArrayInputStream(loaded.audio))
                if (AudioSystem.isConversionSupported(targetFormat, source.format)) {
                    val stream = AudioSystem.getAudioInputStream(targetFormat, source)
                    format = targetFormat
                    audio = stream.readAllBytes()
                    stream.close()
                } else {
                    Showbiz.log.warn("Unsupported audio format for the show '$filename'. It might appear broken")
                    format = source.format
                    audio = loaded.audio
                }
            }

            // Parsing signal data
            val current = mutableListOf<BitId>()
            for (s in loaded.signal) {
                if (s == 0) {
                    signal.add(current.toBitIdArray())
                    current.clear()
                } else {
                    current.add(s.toBitId())
                }
            }

            // Reading video
            val videoPath = getShowVideo(path)
            if (videoPath != null && FFmpeg.localAvailable) {
                val result = CoroutineScope(Dispatchers.IO).launch {
                    this@ShowData.videoInputInfo = FFmpeg.probeVideo(videoPath)
                    this@ShowData.video = FFmpeg.openVideoStream(videoInputInfo!!, FFmpeg.VideoInfo(videoPath, 128, 128, 24.0), Duration.ZERO)
                }
                result.invokeOnCompletion {
                    if (it != null || videoInputInfo == null)
                        Showbiz.log.error("Failed to read video", it ?: Throwable("Unknown error"))
                }
                result.join()
            }
        }
        loadJob?.invokeOnCompletion { err -> owner.level?.server?.execute {
            loading = false
            if (audio.isNotEmpty() && err == null) {
                isLoaded = true
                Showbiz.log.info("Loaded show! (signal=${signal.size}, audio=${audio.size}, video=$videoInputInfo)")
                onLoadOrErr.invoke(this, null)
                owner.markDirtyNotifyAll()
                return@execute
            }
            Showbiz.log.error("Show '${filename}' failed to load! (signal=${signal.size}, audio=${audio.size}, video=$videoInputInfo)", err)
            val errComp = Component.empty()
                .append(Component.translatable("message.showbiz.show_load_fail"))
                .append(Component.literal(": ${err?.message}"))
                .withStyle(ChatFormatting.RED)
            onLoadOrErr.invoke(null, errComp)
            owner.markDirtyNotifyAll()
        } }
    }

    fun loadNBT(tag: CompoundTag?) {
        if (tag == null) return
        val oldName = name
        id = tag.getUUIDOrNull("show_id")
        name = tag.getStringOrNull("show_name")
        isLoaded = tag.getBooleanOrNull("is_loaded") ?: false
        mapping = tag.getStringOrNull("show_mapping")
        /*if (id == null || name == null) {
            Showbiz.log.error("Missing one of: id=$id, name=$name")
            return
        }

        // TODO: Load the show from metadata. Currently VERY buggy
        val notUseless = (oldName != name && !owner.playing)
        if (notUseless || isEmpty() && !loading && !owner.playing) {  // TODO: Figure out a way to bind IDs to shows
            println("Reloading from ${ShowData::loadNBT.name} ($oldName != $name)")
            load(name!!)
        }*/
    }
    fun saveNBT(tag: CompoundTag) {
        id?.let { tag.putUUID("show_id", it) }
        name?.let { tag.putString("show_name", it) }
        mapping?.let { tag.putString("show_mapping", it) }
        tag.putBoolean("is_loaded", isLoaded)
    }

    /** Saves the show to the disk */
    fun saveToDisk(toNotify: Player? = null) {
        val filename = name ?: return
        val showFormat = RshowFormat()

        // Signal
        val signalInt = IntArray(this.signal.sumOf { it.size + 1 })
        var i = 0
        for (array in this.signal) {
            for (bitId in array) signalInt[i++] = bitId.toInt()
            signalInt[i++] = 0
        }
        if (signalInt.isEmpty()) {
            toNotify?.let { Sounds.bad(it) }
            val message = "Failed to save show '$filename' (signal=0)"
            toNotify?.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), true)
            Showbiz.log.info(message)
            return
        }

        // Audio
        val audio = run {
            val format = this.format ?: return@run this.audio
            if (this.audio.isEmpty()) return@run this.audio
            val buf = ByteArrayOutputStream()
            AudioSystem.write(
                AudioSystem.getAudioInputStream(
                    format,
                    AudioInputStream(ByteArrayInputStream(this.audio), format, this.audio.size.toLong() / format.frameSize)
                ),
                AudioFileFormat.Type.WAVE,
                buf
            )
            buf.toByteArray()
        }

        // Finally saving
        val path = getFilePath(filename)
        runCatching {
            Files.newOutputStream(path).use {
                showFormat.write(it, RawShowData(audio = audio, signal = signalInt))
            }
        }.onFailure { Showbiz.log.error("Exception writing show file '${path}'", it) }
        val info = "signal=${signalInt.size}, audio=${audio.size}"
        toNotify?.displayClientMessage(Component.literal("Saved ($info)").withStyle(ChatFormatting.GRAY), true)
        Showbiz.log.info("Saved show! ($info)")
    }

    /** Frees / deletes and resets everything regarding the show data */
    fun unload(toNotify: Player? = null) {
        if (owner.recording && !(owner.level?.isClientSide ?: true)) saveToDisk(toNotify)
        loadJob?.cancel()
        loadJob = null
        owner.recording = false
        signal.clear()
        audio = ByteArray(0)
        isLoaded = false
        loading = false
        id = null
        name = null
        mapping = null
        videoInputInfo = null
        video?.close()
        System.gc()
    }
}