package com.flooferland.showbiz.utils

import net.minecraft.resources.*

fun ResourceLocation.toPath(): ResourcePath =
    ResourcePath(this.namespace, this.path)

/** A [ResourceLocation] that is more optimized to do path-related things */
class ResourcePath(val namespace: String, path: String) {
    private val pathArray = path.split("/", "\\").toMutableList()  // Copy

    val path: String
        get() = pathArray.joinToString("/")

    val parent: ResourcePath
        get() = ResourcePath(namespace, pathArray.copy().also { it.removeLast() }.joinToString ("/"))

    val last: String
        get() = pathArray.last()

    val name: String
        get() = last.split(".").first()

    fun resolve(other: String): ResourcePath =
        this / other

    override fun toString(): String =
        "$namespace:${path}"

    operator fun div(other: String): ResourcePath =
        ResourcePath(namespace, pathArray.joinToString("/").plus("/$other"))

    fun toLocation(): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(namespace, path)
}