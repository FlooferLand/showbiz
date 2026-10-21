package com.flooferland.showbiz.addons

import net.minecraft.*
import net.minecraft.network.chat.*

sealed class AddonErr {
    class FailedToParse(val thing: String) : AddonErr() {
        override fun getText(): MutableComponent =
            Component.literal("Failed to parse ").append(quote(thing))
    }
    class FailedToFind(val thing: String) : AddonErr() {
        override fun getText(): MutableComponent =
            Component.literal("Failed to find ").append(quote(thing))
    }

    class ChainMissingTexture(val botId: String) : AddonErr() {
        override fun getText(): MutableComponent =
            bot(botId).append(" has [chains] but no texture to cut them from")
    }
    class BotMissingAsset(val botId: String, val problem: String) : AddonErr() {
        override fun getText(): MutableComponent =
            bot(botId).append(" was found, but ${problem.lowercase()}")
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