package com.flooferland.showbiz.types

import com.flooferland.showbiz.UpdateChecker

/** Actually diabolical there is nothing in Java or Kotlin that already does this */
data class SemVer(val string: String) {
    val major: Int?
    val minor: Int?
    val patch: Int?

    init {
        // Go fuck yourself Gradle
        val string = if (string == $$"$version") { UpdateChecker.currentVersion } else string

        val split = string.split('.')
        major = split.getOrNull(0)?.toIntOrNull()
        minor = split.getOrNull(1)?.toIntOrNull()
        patch = split.getOrNull(2)?.split("-")?.getOrNull(0)?.toIntOrNull()
    }

    override fun toString() = string

    fun lesserThan(other: SemVer): Boolean? {
        if (major == null || minor == null || patch == null) return null
        if (other.major == null || other.minor == null || other.patch == null) return null
        return when {
            major != other.major -> major < other.major
            minor != other.minor -> minor < other.minor
            else -> patch < other.patch
        }
    }
}