package com.flooferland.showbiz.show

/** One frame of signal data, split per drawer */
class SignalFrame {
    var raw: BitIdArray = bitIdArrayOf()

    fun frameHas(id: BitId): Boolean =
        raw.contains(id)
    fun frameHas(id: Int): Boolean =
        raw.contains(id.toBitId())

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