package com.flooferland.showbiz.types

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import com.flooferland.showbiz.components.PlushComponent
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder

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

        fun of(location: ResourceLocation): ResourceId {
            return ResourceId(location.namespace, location.path)
        }
        fun of(string: String): ResourceId? {
            val split = string.split(':', limit = 2)
            if (split.size != 2) return null
            return ResourceId(split[0], split[1])
        }

        val CODEC: Codec<ResourceId> = Codec.STRING.comapFlatMap(
            { str ->
                val result = ResourceId.of(str)
                result?.let { DataResult.success(it) } ?: DataResult.error { "Failed to get a ResourceId" }
            },
            { it.toString() }
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ResourceId> = StreamCodec.of(
            { buf, opt ->
                buf.writeUtf(opt.namespace)
                buf.writeUtf(opt.path)
            },
            { buf ->
                val namespace = buf.readUtf()
                val path = buf.readUtf()
                ResourceId(namespace, path)
            }
        )
    }
}