package com.flooferland.showbiz.addons.assets

import BitMapping
import com.flooferland.showbiz.show.BitId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** yes this needs an entire deserializer */
object BitmapSerializer : KSerializer<Map<BitId, BitMapping>> {
    private val delegate = MapSerializer(String.serializer(), BitMapping.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Map<BitId, BitMapping> {
        return decoder.decodeSerializableValue(delegate)
            .mapKeys { (k, _) ->
                k.toShortOrNull()
                    ?: throw SerializationException("Invalid bit ID '$k'. Must be a number under 255 (ex: 166)")
            }
    }

    override fun serialize(encoder: Encoder, value: Map<BitId, BitMapping>) {
        val stringed = value.entries.associate { (k, v) -> k.toString() to v }
        encoder.encodeSerializableValue(delegate, stringed)
    }
}