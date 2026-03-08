package com.flooferland.showbiz.types.math

data class Vec2f(var x: Float = 0f, var y: Float = 0f) {
    companion object {
        val ZERO get() = Vec2f()
    }
}