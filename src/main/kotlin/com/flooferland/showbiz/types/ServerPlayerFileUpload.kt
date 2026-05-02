package com.flooferland.showbiz.types

import it.unimi.dsi.fastutil.bytes.ByteArrayList

data class ServerPlayerFileUpload(val filename: String, val maxSizeBytes: Long, val bytes: ByteArrayList = ByteArrayList(maxSizeBytes.toInt())) {
    fun getSizeBytes() = bytes.size.toLong() * Byte.SIZE_BYTES
}
