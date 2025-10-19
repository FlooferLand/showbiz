package com.flooferland.showbiz.show

/** One frame of signal data, split per drawer */
class SignalFrame {
    private var high: Int = 0
    private var low: Int = 0

    companion object {
        const val NEXT_DRAWER: Int = 150  // To convert from/to bottom and top drawer bits, this is added to them
    }

    fun highDrawerHas(bit: Int): Boolean =
        high == bit
    fun lowDrawerHas(bit: Int): Boolean =
        low == bit
    fun anyDrawerHas(bit: Int): Boolean =
        if (Drawer.fromBit(bit) == Drawer.Top)
            high == bit
        else
            low == (bit - NEXT_DRAWER)

    fun arr(): IntArray =
        intArrayOf(high, low)
    fun setFrom(array: IntArray?) {
        array?.getOrNull(1)?.let { high = it }
        array?.getOrNull(0)?.let { low = it }
    }
    fun setFromOne(bit: Int) {
        if (bit < NEXT_DRAWER) {
            high = bit
        } else {
            low = bit
        }
    }
}