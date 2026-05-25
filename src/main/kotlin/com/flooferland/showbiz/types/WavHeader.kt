package com.flooferland.showbiz.types

object WavHeader {
    fun isWav(bytes: ByteArray): Boolean {
        val riff = "RIFF".toByteArray(Charsets.US_ASCII)
        return bytes.size > 4 && bytes.sliceArray(0..3).contentEquals(riff)
    }
}