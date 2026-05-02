package com.flooferland.showbiz.show

// TODO: Test the performance of switching this to a data class, an inline value class, and using BitSet instead of BitIdArray

/** One frame of signal data, split per drawer */
class SignalFrame {
    var raw: BitIdArray = bitIdArrayOf()

    fun frameHas(id: BitId): Boolean =
        raw.contains(id)
    fun frameHas(id: Int): Boolean =
        raw.contains(id.toBitId())

    fun clone() = SignalFrame().also { it.raw = raw }
    fun reset() {
        raw = bitIdArrayOf()
    }
    fun set(array: BitIdArray?) {
        array?.let { raw = array }
    }
    operator fun plusAssign(other: SignalFrame) {
        raw = MutableList<BitId>(raw.size + other.raw.size) { 0u }.also { list ->
            list.addAll(raw)
            list.addAll(other.raw.filter { !raw.contains(it) })
        }.toBitIdArray()
    }
    fun save(): IntArray = raw.map { it.toInt() }.toIntArray()
    fun load(array: IntArray?) {
        array?.let {
            raw = array.map { it.toBitId() }.toBitIdArray()
        }
    }
}