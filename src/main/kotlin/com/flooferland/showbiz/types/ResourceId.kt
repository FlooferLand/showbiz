package com.flooferland.showbiz.types

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation

/** Like [net.minecraft.resources.ResourceLocation] but safer and lighter weight */
data class ResourceId(val namespace: String, val path: String) {
    override fun toString() = "$namespace:$path"
    override fun equals(other: Any?) = when {
        other === this -> true
        other is ResourceId -> other.namespace == namespace && other.path == path
        other is ResourcePath -> other.namespace == namespace && other.path == path
        else -> false
    }

    override fun hashCode(): Int {
        var result = namespace.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    fun matches(other: String) = other == toString()
    fun matches(other: ResourceLocation) = other.namespace == namespace && other.path == path

    companion object {
        fun encode(buf: FriendlyByteBuf, ref: ResourceId) {
            buf.writeUtf(ref.namespace)
            buf.writeUtf(ref.path)
        }
        fun decode(buf: FriendlyByteBuf) = ResourceId(
            namespace = buf.readUtf(),
            path = buf.readUtf()
        )

        fun of(string: String): ResourceId? {
            val split = string.split(':', limit = 2)
            if (split.size != 2) return null
            return ResourceId(split[0], split[1])
        }
    }
}