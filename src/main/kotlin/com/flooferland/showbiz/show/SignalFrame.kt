package com.flooferland.showbiz.show

/** One frame of signal data, split per drawer */
class SignalFrame {
    var raw: BitIdArray = bitIdArrayOf(0)

    companion object {
        /** To convert from/to bottom and top drawer bits, this is added to them */
        val NEXT_DRAWER: BitId = 150.toBitId()
    }

    fun frameHas(id: BitId): Boolean =
        raw.contains(id)

    fun reset() {
        raw = bitIdArrayOf()
    }
    fun set(array: BitIdArray?) {
        array?.let { raw = array }
    }
    fun save(): IntArray = raw.map { it.toInt() }.toIntArray()
    fun load(array: IntArray?) {
        array?.let {
            raw = array.map { it.toBitId() }.toBitIdArray()
        }
    }
}