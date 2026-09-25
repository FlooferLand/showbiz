package com.flooferland.showbiz.handbook

import net.minecraft.resources.*
import net.minecraft.server.packs.resources.*
import net.minecraft.util.profiling.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.handbook.Handbook.cache
import com.flooferland.showbiz.utils.rl
import com.google.gson.Gson
import com.google.gson.JsonElement
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener

private val GSON = Gson()

object HandbookReloadListener : SimpleJsonResourceReloadListener(GSON, "handbook"), IdentifiableResourceReloadListener {
    override fun getFabricId() = rl("handbook")

    override fun apply(obj: Map<ResourceLocation, JsonElement>, resourceManager: ResourceManager, profiler: ProfilerFiller) {
        cache.items.clear()
        cache.bots.clear()
        for ((location, element) in obj) {
            val path = location.path.split('/')
            if (path.size < 3) continue
            val dir = path.getOrNull(0) ?: continue
            val key = path.getOrNull(1)?.let { location.withPath(it) } ?: continue
            val lang = path.getOrNull(2) ?: continue

            try {
                val entry = GSON.fromJson(element, HandbookEntry::class.java)
                when (dir) {
                    "items" -> cache.items.put(key, lang, entry)
                    "bots" -> cache.bots.put(key, lang, entry)
                    else -> error("Unrecognized dir \"$dir\"")
                }
            } catch (e: Exception) {
                error("Failed to read handbook item '$location'", e)
            }
        }

        info("Loaded ${cache.items.size} item entries")
        info("Loaded ${cache.bots.size} bot entries")
    }

    fun info(text: String) {
        Showbiz.log.info("Handbook: $text")
    }
    fun error(text: String, err: Exception? = null) {
        Showbiz.log.error("Handbook: $text", err)
    }
}