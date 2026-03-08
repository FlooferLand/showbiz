package com.flooferland.showbiz.types.math

/** Like [Vec3f], but the values are final */
data class Vec3fc(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    fun withX(x: Float) =
        Vec3fc(x, y)
    fun withY(y: Float) =
        Vec3fc(x, y)

    companion object {
        val ZERO get() = Vec3fc()
    }
}