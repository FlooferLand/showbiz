@file:Suppress("NOTHING_TO_INLINE")

package com.flooferland.showbiz.show

// Should probably use UByte, but its experimental since JVM doesn't really support unsignedness

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

inline fun List<BitId>.toBitIdArray() =
    toUShortArray()
