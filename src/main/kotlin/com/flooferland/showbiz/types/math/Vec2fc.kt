package com.flooferland.showbiz.types.math

/** Immutable float Vector2 */
data class Vec2fc(val x: Float = 0f, val y: Float = 0f) {
    companion object {
        val ZERO get() = Vec2fc()
    }
}