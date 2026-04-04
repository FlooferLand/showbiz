package com.flooferland.showbiz.addons.data

import net.minecraft.network.FriendlyByteBuf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddonBotEntry(
    val name: String,
    val authors: List<String>,
    val accepts: AcceptedFormats,

    @SerialName("allow_missing")
    val allowMissing: Boolean = false
) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(name)
        buf.writeUtf(authors.joinToString(", "))
        accepts.encode(buf)
    }
    companion object {
        fun decode(buf: FriendlyByteBuf) = AddonBotEntry(
            name = buf.readUtf(),
            authors = buf.readUtf().split(", "),
            accepts = AcceptedFormats.decode(buf)
        )
    }
}

// TODO: Enforce accepted formats so people can't load fshw files for RAE bots
@Serializable
data class AcceptedFormats(
    val rockafire: Boolean = false,
    val fnaf1: Boolean = false,
    val other: Boolean = false
) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeBoolean(rockafire)
        buf.writeBoolean(fnaf1)
        buf.writeBoolean(other)
    }
    companion object {
        fun decode(buf: FriendlyByteBuf) = AcceptedFormats(
            rockafire = buf.readBoolean(),
            fnaf1 = buf.readBoolean(),
            other = buf.readBoolean()
        )
    }
}
