package com.flooferland.showbiz.types.math

/** Mutable int Vector2 */
data class Vec2i(var x: Int = 0, var y: Int = 0) {
    operator fun plusAssign(size: Vec2i) {
        x += size.x
        y += size.y
    }
    operator fun minusAssign(size: Vec2i) {
        x -= size.x
        y -= size.y
    }
    operator fun divAssign(size: Vec2i) {
        x /= size.x
        y /= size.y
    }
    operator fun timesAssign(size: Vec2i) {
        x *= size.x
        y *= size.y
    }

    companion object {
        val ZERO get() = Vec2i()
    }
}