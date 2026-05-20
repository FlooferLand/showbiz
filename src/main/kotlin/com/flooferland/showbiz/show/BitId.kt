@file:Suppress("NOTHING_TO_INLINE")

package com.flooferland.showbiz.show

import net.minecraft.network.FriendlyByteBuf

// Should probably use UByte, but its experimental since the JVM doesn't really support unsignedness

typealias BitId = UShort
typealias BitIdArray = UShortArray
inline fun bitIdArrayOf(vararg ids: BitId): BitIdArray {
    val array = BitIdArray(ids.size)
    ids.forEachIndexed { index, value -> array[index] = value }
    return array
}

inline fun String.toBitIdOrNull(): BitId? =
    toUShortOrNull()

inline fun Int.toBitId(): BitId =
    toUShort()
inline fun Short.toBitId(): BitId =
    toUShort()
inline fun Byte.toBitId(): BitId =
    toUShort()

inline fun Collection<BitId>.toBitIdArray() =
    toUShortArray()
inline fun FriendlyByteBuf.writeBitId(bitId: BitId) =
    writeShort(bitId.toInt())
inline fun FriendlyByteBuf.readBitId(): BitId =
    readShort().toBitId()
