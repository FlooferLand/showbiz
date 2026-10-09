package com.flooferland.showbiz.addons.data

class ChainLayout(
    val links: Array<Array<String>>,
    val roots: Array<String>,
    val owners: Array<String>,
    val lagHash: IntArray
) {
    val size: Int get() = roots.size
    val linkCounts: IntArray = IntArray(links.size) { links[it].size }
    val bones: Set<String> = roots.toHashSet().also { set -> links.forEach { set.addAll(it) } }

    companion object {
        val EMPTY: ChainLayout = ChainLayout(emptyArray(), emptyArray(), emptyArray(), IntArray(0))
    }
}