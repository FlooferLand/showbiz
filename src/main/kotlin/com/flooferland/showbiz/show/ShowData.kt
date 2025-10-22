package com.flooferland.showbiz.show

import com.flooferland.bizlib.formats.RshowFormat
import com.flooferland.showbiz.FileStorage
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import com.flooferland.showbiz.utils.Extensions.getUUIDOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.nbt.*
import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.Path

// Should probably use UByte, but its experimental since JVM doesn't really support unsignedness

/**
 * Abstraction class to work with rshw.
 * Every rshw frame is a list of currently played bits, collecting that at load time is way nicer.
 */
class ShowData(val owner: PlaybackControllerBlockEntity) {
    // TODO: Convert signal to a list of longs and pack bit ids using bitwise operations
    val signal: MutableList<ByteArray> = ArrayList()
    var audio: ByteArray = ByteArray(0)
    var id: UUID? = null
    var name: String? = null

    var loading = false

    fun isEmpty() = signal.isEmpty() || audio.isEmpty()

    fun load(filename: String, playOnLoad: Boolean = false) {
        reset()
        loading = true
        name = filename
        id = UUID.randomUUID()

        val coro = CoroutineScope(Dispatchers.IO).launch {
            val stream = Files.newInputStream(Path("${FileStorage.SHOWS_DIR}/$filename"))
            val loaded = RshowFormat().read(stream)
            audio = loaded.audio

            // Parsing signal data
            val current = mutableListOf<Byte>()
            for (s in loaded.signal) {
                if (s == 0) {
                    signal.add(current.toByteArray())
                    current.clear()
                } else {
                    current.add(s.toByte())
                }
            }
        }

        coro.invokeOnCompletion { err ->
            loading = false
            if (err == null) {
                Showbiz.log.info("Loaded show! (signal=${signal.size}, audio=${audio.size})")
                if (playOnLoad) owner.playing = true
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
    }

    fun reset() {
        signal.clear()
        audio = ByteArray(0)
        id = null
        name = null
    }
}