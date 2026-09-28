package com.flooferland.showbiz.handbook

import net.minecraft.resources.*

object Handbook {
    val cache = Cache()
    public data class Cache(
        val items: Category = Category(),
        val bots: Category = Category()
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
}