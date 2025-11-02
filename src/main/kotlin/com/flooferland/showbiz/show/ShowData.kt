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
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.UUID
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.io.path.Path

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

    fun isEmpty() = !isLoaded

    fun load(filename: String, onLoad: (() -> Unit)? = null) {
        reset()
        loading = true
        name = filename
        id = UUID.randomUUID()
        mapping = run {  // TODO: Clean up the code for mappings
            val format = name?.split('.', limit = 2)?.last() ?: run {
                Showbiz.log.error("Error loading show '${filename}'. Format is missing a file extension")
                return
            }
            when (format) {  // TODO: Move mapping file names to file types to its own thingy
                "fshw" -> "faz"
                "rshw" -> "rae"
                else -> {
                    Showbiz.log.error("Error loading show '${filename}'. Format '${format}' is not supported")
                    return
                }
            }
        }

        val coro = CoroutineScope(Dispatchers.IO).launch {
            val loaded = run {
                val stream = Files.newInputStream(Path("${FileStorage.SHOWS_DIR}/$filename"))
                val out = RshowFormat().read(stream)
                stream.close()
                out
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
            isLoaded = (audio.isNotEmpty() && signal.isNotEmpty())
            if (!isLoaded) {
                Showbiz.log.error("Show failed to load! (signal=${signal.size}, audio=${audio.size})")
                return@invokeOnCompletion
            }
            if (err == null) {
                Showbiz.log.info("Loaded show! (signal=${signal.size}, audio=${audio.size})")
                onLoad?.invoke()
            } else {
                Showbiz.log.error(err.toString())
            }
        }
    }

    fun loadNBT(tag: CompoundTag?) {
        if (tag == null) return
        val oldName = name
        id = tag.getUUIDOrNull("Show-Id")
        name = tag.getStringOrNull("Show-Name")
        isLoaded = tag.getBooleanOrNull("Is-Loaded") ?: false
        mapping = tag.getStringOrNull("Show-Mapping")
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
        id?.let { tag.putUUID("Show-Id", it) }
        name?.let { tag.putString("Show-Name", it) }
        mapping?.let { tag.putString("Show-Mapping", it) }
        tag.putBoolean("Is-Loaded", isLoaded)
    }

    fun reset() {
        signal.clear()
        audio = ByteArray(0)
        isLoaded = false
        id = null
        name = null
        mapping = null
    }
}