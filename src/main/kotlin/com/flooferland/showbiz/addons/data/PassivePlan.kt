package com.flooferland.showbiz.addons.data

class PassivePlan(
    val names: Array<String>,
    val parentIndex: IntArray
) {
    val size: Int get() = names.size

    var passiveIndices: IntArray = IntArray(0)
    var passiveResolved: Boolean = false

    //scratch for the ancestor prefix sum (render thread only, one animatable at a time)
    val accScratch: FloatArray = FloatArray(names.size * 3)

    companion object {
        val EMPTY: PassivePlan = PassivePlan(emptyArray(), IntArray(0))
    }
}