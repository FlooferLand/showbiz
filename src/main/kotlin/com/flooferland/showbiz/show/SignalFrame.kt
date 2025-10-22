package com.flooferland.showbiz.show

/** One frame of signal data, split per drawer */
class SignalFrame {
    var raw: ByteArray = ByteArray(0)

    companion object {
        const val NEXT_DRAWER: Byte = 150.toByte()  // To convert from/to bottom and top drawer bits, this is added to them
    }

    fun frameHas(bitId: Byte): Boolean =
        raw.contains(bitId)

    fun reset() {
        raw = byteArrayOf()
    }
    fun save(): ByteArray = raw
    fun load(array: ByteArray?) {
        array?.let { raw = it }
    }
}