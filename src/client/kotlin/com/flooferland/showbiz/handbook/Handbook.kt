package com.flooferland.showbiz.handbook

import com.flooferland.showbiz.handbook.HandbookXml.Styling
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation

object Handbook {
    val cache = Cache()

    public data class Cache(
        val items: Category = Category(),
        val bots: Category = Category(),
        var styles: Map<String, HandbookXml.StyleDefs> = mapOf()
    )

    public class Category {
        val entries: HashMap<ResourceLocation, HandbookEntry> = hashMapOf()
        val size get() = entries.size

        fun clear() { entries.clear() }
        fun has(key: ResourceLocation?): Boolean {
            return get(key) != null
        }
        fun get(key: ResourceLocation?): HandbookEntry? {
            if (key == null) return null
            return entries[key]
        }
        fun put(key: ResourceLocation, entry: HandbookEntry) {
            entries[key] = entry
        }
    }

    object Style {
        fun getComp(text: Component, refs: HandbookXml.StyleDefs): MutableComponent {
            var text = text.copy()
            for (style in refs.entries) {
                text = text.withStyle { textStyle ->
                    when (style) {
                        is Styling.Foreground -> {
                            val color = style.color.let { ChatFormatting.getByName(it) } ?: ChatFormatting.WHITE
                            textStyle.withColor(color)
                        }
                        is Styling.Background -> {
                            textStyle
                        }
                    }
                }
            }
            return text
        }
    }
}