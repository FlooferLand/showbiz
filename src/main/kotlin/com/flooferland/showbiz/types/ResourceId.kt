package com.flooferland.showbiz.types

import net.minecraft.network.FriendlyByteBuf

/** Like [net.minecraft.resources.ResourceLocation] but safer and lighter weight */
data class ResourceId(val namespace: String, val path: String) {
    override fun toString() = "$namespace:$path"
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