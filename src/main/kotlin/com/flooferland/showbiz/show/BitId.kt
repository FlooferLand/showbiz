@file:Suppress("NOTHING_TO_INLINE")
package com.flooferland.showbiz.show

// Should probably use UByte, but its experimental since JVM doesn't really support unsignedness

typealias BitId = Short
typealias BitIdArray = ShortArray
inline fun bitIdArrayOf(vararg ids: Short): BitIdArray = BitIdArray(ids.size) { i -> ids[i] }

inline fun Int.toBitId(): BitId =
    toShort()

inline fun List<BitId>.toBitIdArray() =
    toShortArray()
