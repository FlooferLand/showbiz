package com.flooferland.showbiz.show

import com.flooferland.bizlib.formats.RshowFormat
import com.flooferland.showbiz.Showbiz
import java.io.InputStream
import java.util.UUID

// Should probably use UByte, but its experimental since JVM doesn't really support unsignedness

/**
 * Abstraction class to work with rshw.
 * Every rshw frame is a list of currently played bits, collecting that at load time is way nicer.
 */
class ShowData {
    // TODO: Convert signal to a list of longs and pack bit ids using bitwise operations
    val signal: MutableList<ByteArray> = ArrayList()
    var audio: ByteArray = ByteArray(0)
    var id: UUID? = null

    fun isEmpty() = signal.isEmpty() || audio.isEmpty()

    fun load(toLoad: InputStream) {
        signal.clear()
        id = UUID.randomUUID()

        val loaded = RshowFormat().read(toLoad)
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

        // Notification
        Showbiz.log.info("Loaded show! (signal=${signal.size}, audio=${audio.size})")
    }
}