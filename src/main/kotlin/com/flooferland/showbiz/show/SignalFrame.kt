package com.flooferland.showbiz.show

import net.minecraft.network.*

// TODO: Test the performance of using an inline value class and using BitSet instead of BitIdArray

/** One frame of signal data, split per drawer */
class SignalFrame(private var raw: BitIdArray = bitIdArrayOf()) : Cloneable, Collection<BitId> {
    override val size get() = raw.size

    public fun frameHas(id: BitId): Boolean = raw.contains(id)
    public fun frameHas(id: Int): Boolean = raw.contains(id.toBitId())
    public fun reset() {
        raw = bitIdArrayOf()
    }
    fun set(array: BitIdArray?) {
        array?.let { raw = array }
    }
    operator fun plusAssign(other: SignalFrame) {
        val combined = raw.toMutableSet()
        other.raw.forEach { combined.add(it) }
        raw = combined.toBitIdArray()
    }
    operator fun plusAssign(other: Collection<BitId>) {
        val combined = raw.toMutableSet()
        other.forEach { combined.add(it) }
        raw = combined.toBitIdArray()
    }

    public fun saveTo(buf: FriendlyByteBuf) {
        buf.writeVarIntArray(raw.map { it.toInt() }.toIntArray())
    }
    public fun loadFrom(buf: FriendlyByteBuf) {
        val array = buf.readVarIntArray() ?: return
        raw = array.map { it.toBitId() }.toBitIdArray()
    }

    override fun isEmpty() = size == 0
    override fun contains(element: BitId) = raw.contains(element)
    override fun containsAll(elements: Collection<BitId>) = raw.containsAll(elements)
    override fun iterator() = raw.iterator()

    public override fun clone() = SignalFrame(raw)
    override fun toString() = raw.contentToString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignalFrame) return false
        if (!raw.contentEquals(other.raw)) return false
        return true
    }
    override fun hashCode(): Int {
        return raw.hashCode()
    }
}