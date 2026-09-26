package com.flooferland.showbiz.handbook

import net.minecraft.client.*
import net.minecraft.resources.*

object Handbook {
    val cache = Cache()
    public data class Cache(
        val items: Category = Category(),
        val bots: Category = Category()
    )
    public class Category {
        val entries: HashMap<ResourceLocation, HashMap<String, HandbookEntry>> = hashMapOf()
        val size get() = entries.size

        fun clear() { entries.clear() }
        fun has(key: ResourceLocation?): Boolean {
            return get(key) != null
        }
        fun get(key: ResourceLocation?): HandbookEntry? {
            if (key == null) return null
            val lang = Minecraft.getInstance().languageManager.selected
            val entry = entries[key] ?: return null
            return entry[lang] ?: entry["en_us"]
        }
        fun put(key: ResourceLocation, lang: String, entry: HandbookEntry) {
            val map = entries.getOrPut(key) { hashMapOf() }
            map[lang] = entry
        }
    }

}