package com.flooferland.showbiz.types

enum class AudioChannel {
    Mono, Left, Right;

    val id: String by lazy {
        when (this) {
            Mono -> "mono"
            Left -> "left"
            Right -> "right"
        }
    }

    companion object {
        val DEFAULT = AudioChannel.Mono
        fun from(id: String): AudioChannel? = when (id) {
            "mono" -> Mono
            "left" -> Left
            "right" -> Right
            else -> null
        }
    }
}