package com.flooferland.showbiz.addons

import com.flooferland.showbiz.utils.Extensions.ifNotEmpty
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation

// Note to self: USE THE ADDON ERROR FUNCTION FOR EXCEPTIONS!!
// It already handles exceptions, don't shove them into the errors themselves

sealed class AddonErr {
    class FailedToParse(val thing: String, val info: String = "") : AddonErr() {
        override fun getText(): MutableComponent =
            Component.literal("Failed to parse ").append(quote(thing)).append(info.ifNotEmpty { ": $info" })
    }
    class FailedToFind(val thing: String, val info: String = "") : AddonErr() {
        override fun getText(): MutableComponent =
            Component.literal("Failed to find ").append(quote(thing)).append(info.ifNotEmpty { ": $info" })
    }

    class ChainMissingTexture(val botId: String) : AddonErr() {
        override fun getText(): MutableComponent =
            bot(botId).append(" has [chains] but no texture to cut them from")
    }
    class BotMissingVariant(val botId: String, val variant: String, val asset: ResourceLocation? = null, val thing: String) : AddonErr() {
        override fun getText(): MutableComponent =
            bot(botId).append(" has a variant ").append(quote(variant)).append(" whose $thing doesn't exist")
                .also { text -> asset?.let { text.append(": $it") } }
    }
    class IdWrongCase(val botId: String, val id: String, val thing: String) : AddonErr() {
        override fun getText(): MutableComponent =
            bot(botId).append(" uses the $thing ID ").append(quote(id)).append(", which doesn't follow the ").append(quote("snake_case")).append(" naming convention")
    }

    class Other(val error: String) : AddonErr() {
        override fun getText(): MutableComponent = Component.literal(error)
    }

    init { entries += this }

    abstract fun getText(): MutableComponent

    companion object {
        val entries = mutableListOf<AddonErr>()

        fun bot(botId: String): MutableComponent =
            Component.literal("Bot ").append(quote(botId))

        fun quote(string: String): MutableComponent =
            Component.literal("'$string'").withStyle(ChatFormatting.GRAY)
    }
}