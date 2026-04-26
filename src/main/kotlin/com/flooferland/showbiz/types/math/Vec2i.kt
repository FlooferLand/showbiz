package com.flooferland.showbiz.types.math

/** Mutable int Vector2 */
data class Vec2i(var x: Int = 0, var y: Int = 0) {
    companion object {
        val ZERO get() = Vec2i()
    }
}