package com.flooferland.showbiz.types.math

/** Immutable int Vector2 */
data class Vec2ic(val x: Int = 0, val y: Int = 0) {
    companion object {
        val ZERO get() = Vec2ic()
    }
}