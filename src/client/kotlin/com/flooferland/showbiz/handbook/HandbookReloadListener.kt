package com.flooferland.showbiz.handbook

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.handbook.Handbook.cache
import com.flooferland.showbiz.utils.ShowbizEnv
import com.flooferland.showbiz.utils.rl
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XML

object HandbookReloadListener : SimplePreparableReloadListener<MutableMap<ResourceLocation, HandbookXml.Root>>(), IdentifiableResourceReloadListener {
    const val DIRECTORY = "handbook"
    override fun getFabricId() = rl("handbook")

    fun debug(text: String) {
        Showbiz.log.debug("Handbook: $text")
    }
    fun info(text: String) {
        Showbiz.log.info("Handbook: $text")
    }
    fun error(text: String, err: Exception? = null) {
        ShowbizEnv.devThrow("Handbook: $text", err)
    }

    fun scanDirectory(manager: ResourceManager, name: String, output: MutableMap<ResourceLocation, HandbookXml.Root>) {
        val converter = FileToIdConverter(name, ".xml")
        for ((fileId, value) in converter.listMatchingResources(manager)) {
            val id = converter.fileToId(fileId)
            try {
                value.openAsReader().use { reader ->
                    val elem = XML.v1.decodeFromStream(HandbookXml.Root.serializer(), reader, QName("showbiz:handbook", "root"))
                    val old = output.put(id, elem)
                    check(old == null) { "Duplicate data file ignored with ID $id" }
                }
            } catch (exception: Exception) {
                error("Couldn't parse data file $id from $fileId", exception)
            }
        }
    }

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): MutableMap<ResourceLocation, HandbookXml.Root> {
        val map = hashMapOf<ResourceLocation, HandbookXml.Root>()
        scanDirectory(manager, DIRECTORY, map)
        return map
    }

    override fun apply(obj: MutableMap<ResourceLocation, HandbookXml.Root>, resourceManager: ResourceManager, profiler: ProfilerFiller) {
        cache.items.clear()
        cache.bots.clear()
        for ((location, root) in obj) {
            val path = location.path.split('/')
            if (path.size < 2) continue
            val dir = path.getOrNull(0) ?: continue
            val name = path.getOrNull(1)?.let { location.withPath(it) } ?: continue

            try {
                val entry = HandbookEntry(
                    root.head.summary.toComponent(),
                    root.head.facts.lines.map { it.toComponent() },
                    root.pages.entries
                )
                when (dir) {
                    "items" -> cache.items.put(name, entry)
                    "bots" -> cache.bots.put(name, entry)
                    else -> error("Unrecognized dir \"$dir\"")
                }
            } catch (e: Exception) {
                error("Failed to read handbook item '$location'", e)
            }
        }

        debug("Loaded ${cache.items.size} item entries")
        debug("Loaded ${cache.bots.size} bot entries")
    }
}