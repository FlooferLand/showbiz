package com.flooferland.showbiz.types.physics

//One simulated chain: point masses joined by rigid segments, joint 0 pinned to the hand
class ChainState(@JvmField val segs: Int, @JvmField val links: Int, @JvmField val lagHash: Int) {
    @JvmField val n: Int = segs + 1
    // NOTE: Velocity is never stored, Verlet reads it as (pos - prev). Anything that moves a joint must move its prev too, or it invents velocity
    @JvmField val pos: FloatArray = FloatArray(n * 3)
    @JvmField val prev: FloatArray = FloatArray(n * 3)
    @JvmField var seg: Float = 0.05f
    @JvmField var totalLen: Float = 0f
    @JvmField var ready: Boolean = false
    @JvmField var cosMax: Float = 0f
    @JvmField var cosSoft: Float = 0f

    @JvmField var rootPrevX: Float = 0f
    @JvmField var rootPrevY: Float = 0f
    @JvmField var rootPrevZ: Float = 0f
    @JvmField var rootVelX: Float = 0f
    @JvmField var rootVelY: Float = 0f
    @JvmField var rootVelZ: Float = 0f
    @JvmField var pinX: Float = 0f
    @JvmField var pinY: Float = 0f
    @JvmField var pinZ: Float = 0f

    @JvmField var lagFrames: Int = 0
    @JvmField var asleep: Boolean = false
    @JvmField var anchorSpeed: Float = 0f

    @JvmField var acc: Float = 0f
    @JvmField var stillTime: Float = 0f
    @JvmField var clock: Float = 0f
    @JvmField val drawPrev: FloatArray = FloatArray(n * 3)
    @JvmField val draw: FloatArray = FloatArray(n * 3)

    @JvmField val lagT: FloatArray = FloatArray(LAG_SLOTS)
    @JvmField val lagP: FloatArray = FloatArray(LAG_SLOTS * 3)
    @JvmField var lagN: Int = 0

    @JvmField var restKnown: Boolean = false
    @JvmField val restDir: FloatArray = FloatArray(links * 3)
    @JvmField val renderRest: FloatArray = FloatArray(links * 3)
    @JvmField val lp: FloatArray = FloatArray(n * 3)
    @JvmField val dirs: FloatArray = FloatArray(links * 3)

    @JvmField val euler: FloatArray = FloatArray(links * 3)
    @JvmField val eulerOk: BooleanArray = BooleanArray(links)
    @JvmField var posed: Boolean = false

    companion object {
        const val LAG_SLOTS: Int = 128
    }
}