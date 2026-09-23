package com.flooferland.showbiz.types

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.mojang.serialization.Codec

/**
 * Bits that are mapped to a bit chart.
 *
 * Key = Chart ID
 *
 * Value = Bit ID
 */
data class MappedBits(val inner: HashMap<String, MutableSet<BitId>> = HashMap<String, MutableSet<BitId>>(Showbiz.charts.size)) : AbstractMap<String, MutableSet<BitId>>() {
    override val entries get() = inner.entries
    val charts get() = inner.keys
    val bits get() = inner.values.flatten()

    /** Gets the first chart that contains bits */
    fun getFirstNotEmpty(): Pair<String, MutableSet<BitId>>? {
        charts.forEach { mapping ->
            val bits = inner[mapping] ?: return@forEach
            if (bits.isNotEmpty()) return Pair(mapping, bits)
        }
        return null
    }

    fun getOrPutDefault(key: String) = inner.getOrPut(key) { mutableSetOf() }
    fun addBit(chartId: String, bit: BitId) {
        val bits = getOrPutDefault(chartId)
        bits += bit
        inner[chartId] = bits
    }
    fun removeBit(chartId: String, bit: BitId) {
        val bits = getOrPutDefault(chartId)
        bits -= bit
        inner[chartId] = bits
    }
    fun clearBits(chartId: String) {
        val bits = getOrPutDefault(chartId)
        bits.clear()
        inner[chartId] = bits
    }
    fun chartHasBit(chartId: String?, predicate: (BitId) -> Boolean): Boolean {
        if (chartId == null) return false
        return inner[chartId]?.any(predicate) ?: false
    }
    fun clearCharts() {
        inner.clear()
    }
    fun setBits(chartId: String, bits: MutableSet<BitId>) {
        inner[chartId] = bits
    }
    fun set(received: MappedBits) {
        inner.clear()
        inner.putAll(received)
    }

    companion object {
        val CODEC: Codec<MappedBits> = Codec.unboundedMap(Codec.STRING, Codec.SHORT.listOf()).xmap(
            { map -> MappedBits().apply { map.forEach { (chart, bits) -> bits.forEach { addBit(chart, it.toBitId()) } } } },
            { bits -> bits.charts.associateWith { chart -> bits.getOrPutDefault(chart).map { it.toShort() } } }
        )
    }
}

