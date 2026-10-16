package com.flooferland.showbiz.types

import net.minecraft.nbt.*
import net.minecraft.network.codec.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import kotlin.jvm.optionals.getOrNull

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
    fun clone(): MappedBits {
        val copy = MappedBits()
        forEach { (chartId, bits) -> copy.setBits(chartId, HashSet(bits)) }
        return copy
    }

    fun save(tag: CompoundTag, key: String) {
        MappedBits.CODEC.encodeStart(NbtOps.INSTANCE, this).result().getOrNull()?.let {
            tag.put(key, it)
        }
    }

    fun load(tag: CompoundTag, key: String) {
        clearCharts()

        // Backwards compatibility with pre-0.4.0
        if (tag.contains(key, Tag.TAG_INT_ARRAY.toInt())) {
            tag.getIntArrayOrNull(key)?.forEach {
                addBit(BitChartStore.DEFAULT, it.toBitId())
            }
        } else {
            // Reading post-0.4.0
            tag.get(key)?.let { filterTag ->
                MappedBits.CODEC.parse(NbtOps.INSTANCE, filterTag).result().getOrNull()?.let { loaded ->
                    loaded.charts.forEach { chartId ->
                        loaded.getOrPutDefault(chartId).forEach { addBit(chartId, it) }
                    }
                }
            }
        }
    }


    companion object {
        val CODEC: Codec<MappedBits> = Codec.unboundedMap(Codec.STRING, Codec.SHORT.listOf()).xmap(
            { map -> MappedBits().apply { map.forEach { (chart, bits) -> bits.forEach { addBit(chart, it.toBitId()) } } } },
            { bits -> bits.charts.associateWith { chart -> bits.getOrPutDefault(chart).map { it.toShort() } } }
        )
        val STREAM_CODEC: StreamCodec<ByteBuf, MappedBits> = ByteBufCodecs.fromCodec(CODEC)
    }
}

