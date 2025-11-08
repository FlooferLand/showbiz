package com.flooferland.showbiz.utils

data class Vec3f(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    companion object {
        val ZERO get() = Vec3f()
    }
}
