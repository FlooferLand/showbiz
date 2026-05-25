package com.flooferland.showbiz.types

class BitChartStore {
    val ids = mutableListOf<String>()
    val extensions = mutableListOf<String>()
    val idsToInfo = mutableMapOf<String, BitChartInfo>()
    val extensionToInfo = mutableMapOf<String, BitChartInfo>()
    val extensionToId = mutableMapOf<String, String>()
    val size get() = ids.size

    init {
        add(id = RAE_ID,
            BitChartInfo(extension = "rshw", color = 0xFFAF4F2B.toInt())
        )
        add(id = FAZ_ID,
            BitChartInfo(extension = "fshw", color = 0xFF8D6320.toInt())
        )
        add(id = CEC_ID,
            BitChartInfo(extension = "cshw", color = 0xFF8D6320.toInt())
        )
        add(id = FAZTOYS_ID,
            BitChartInfo(extension = "tshw", color = 0xFF204A8D.toInt())
        )
    }

    fun add(id: String, info: BitChartInfo) {
        ids.add(id)
        extensions.add(info.extension)
        idsToInfo[id] = info
        extensionToInfo[info.extension] = info
        extensionToId[info.extension] = id
    }

    fun getColor(id: String? = null, ext: String? = null): Int =
        id?.let { idsToInfo[it]?.color } ?: ext?.let { extensionToInfo[it]?.color } ?: 0xFFFFFFFF.toInt()

    data class BitChartInfo(val extension: String, val color: Int)

    companion object {
        const val RAE_ID = "rae"
        const val FAZ_ID = "faz"
        const val CEC_ID = "cec"
        const val FAZTOYS_ID = "toy"
        const val FAZMANGLE_ID = "mgl"

        // TODO: Figure out a replacement for having to include a default show mapping in most systems.
        //       Most of the things using this shouldn't have any mapping assigned
        const val DEFAULT = RAE_ID
    }
}