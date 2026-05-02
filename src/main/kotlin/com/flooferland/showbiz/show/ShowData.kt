package com.flooferland.showbiz.show

import com.flooferland.bizlib.formats.RshowFormat
import com.flooferland.showbiz.FileStorage
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import com.flooferland.showbiz.utils.Extensions.getUUIDOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.nbt.*
import com.flooferland.bizlib.RawShowData
import com.flooferland.bizlib.formats.LegacyRshowFormat
import com.flooferland.showbiz.utils.Extensions.applyChange
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.UUID
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.io.path.Path

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
    var id: UUID? = null
    var name: String? = null
    var format: AudioFormat? = null
    var mapping: String? = null

    val targetFormat = AudioFormat(
        44100f,
        16,
        1,
        true,
        false
    )

    var loading = false
    var isLoaded = false

    fun getFilePath(filename: String) = Path("${FileStorage.SHOWS_DIR}/$filename")
    fun isEmpty() = !isLoaded

    fun load(filename: String, onLoad: ((ShowData?) -> Unit)? = null) {
        unload()
        loading = true
        name = filename
        id = UUID.randomUUID()
        mapping = run {
            val ext = name?.split('.', limit = 2)?.last() ?: run {
                Showbiz.log.error("Error loading show '${filename}'. Format is missing a file extension")
                return
            }
            Showbiz.charts.extensionToId[ext] ?: run {
                Showbiz.log.error("Error loading show '${filename}'. Format '${ext}' is not supported")
                return
            }
        }

        Showbiz.log.debug("Loading tape '${name}' ($mapping)")

        val coro = CoroutineScope(Dispatchers.IO).launch {
            val loaded = run {
                val path = getFilePath(filename)
                val out = runCatching { Files.newInputStream(path).use { RshowFormat().read(it) } }
                out.onFailure { throwable ->
                    Showbiz.log.error("BizlibNative failed to load '${path}'", throwable)
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
        }
        coro.invokeOnCompletion { err ->
            loading = false
            isLoaded = audio.isNotEmpty() || err == null
            if (isLoaded) {
                Showbiz.log.info("Loaded show! (signal=${signal.size}, audio=${audio.size})")
                onLoad?.invoke(this)
                return@invokeOnCompletion
            }
            Showbiz.log.error("Show failed to load! (signal=${signal.size}, audio=${audio.size})", err)
            onLoad?.invoke(null)
        }
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

    /** Frees / deletes and resets everything regarding the show data */
    fun unload() {
        if (owner.recording && !(owner.level?.isClientSide ?: true)) run {
            val filename = name ?: return@run
            val showFormat = RshowFormat()

            // Signal
            val signalInt = IntArray(this.signal.sumOf { it.size + 1 })
            var i = 0
            for (array in this.signal) {
                for (bitId in array) signalInt[i++] = bitId.toInt()
                signalInt[i++] = 0
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
            Showbiz.log.info("Saved show! (signal=${signalInt.size}, audio=${audio.size})")
        }
        signal.clear()
        audio = ByteArray(0)
        isLoaded = false
        loading = false
        id = null
        name = null
        mapping = null
        System.gc()
    }
}