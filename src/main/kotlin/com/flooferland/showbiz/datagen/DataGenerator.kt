package com.flooferland.showbiz.datagen

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.datagen.providers.BlockProvider
import com.flooferland.showbiz.datagen.providers.ItemProvider
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.Extensions.blockPath
import com.flooferland.showbiz.utils.Extensions.itemPath
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.relativeTo

object DataGenerator {
    val engaged = runCatching { System.getProperty("$MOD_ID.datagen") }.getOrNull() == "true"

    @JvmStatic
    fun main(args: Array<String>) {
        if (!engaged) return;

        println("Running data generator..")
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
        generate()
    }

    fun generate() {
        val rootPath = Path.of("src", "main", "generated", "resources")
        val assetsRoot = rootPath / "assets" / Showbiz.MOD_ID
        val fileListPath = rootPath / "generated.txt"
        val json = Json { prettyPrint = true }
        val fileList = mutableListOf<Path>()
        fun writeAsset(path: Path, data: JsonObject) {
            path.createParentDirectories()
            println("Writing '${path.relativeTo(rootPath)}'")
            fileList.add(path)
            Files.write(
                path.toAbsolutePath(),
                json.encodeToString(data).encodeToByteString().toByteArray()
            )
        }

        // Generation
        for (block in ModBlocks.entries) {
            // States
            val stateJson = BlockProvider.generateState(block) ?: run {
                print("Skipped block '${block.id}'")
                continue
            }
            val statePath = assetsRoot / "blockstates" / "${block.id.path}.json"
            writeAsset(statePath, stateJson)

            // Block model
            val modelJson = BlockProvider.generateBlockModel(block) ?: continue
            val modelPath = assetsRoot / "models" / "block" / "${block.id.path}.json"
            writeAsset(modelPath, modelJson)

            // Item model
            val itemModelJson = BlockProvider.generateBlockItemModel(block) ?: continue
            val itemModelPath = assetsRoot / "models" / "item" / "${block.id.path}.json"
            writeAsset(itemModelPath, itemModelJson)

            // Item
            val itemJson = ItemProvider.generateItem(block.id.blockPath()) ?: continue
            val itemPath = assetsRoot / "items" / "${block.id.path}.json"
            writeAsset(itemPath, itemJson)
        }
        for (item in ModItems.entries) {
            // Model
            val modelJson = ItemProvider.generateModel(item) ?: run {
                print("Skipped item '${item.id}'")
                continue
            }
            val modelPath = assetsRoot / "models" / "item" / "${item.id.path}.json"
            writeAsset(modelPath, modelJson)

            // Item
            val itemJson = ItemProvider.generateItem(item.id.itemPath()) ?: continue
            val itemPath = assetsRoot / "items" / "${item.id.path}.json"
            writeAsset(itemPath, itemJson)
        }

        // Removing files that weren't in this build
        if (Files.exists(fileListPath)) {
            for (line in Files.readAllLines(fileListPath)) {
                val path = Path.of(line)
                if (!fileList.any { p -> p == path }) {
                    print("Removing '$path'")
                    val path = path.toAbsolutePath()
                    Files.deleteIfExists(path)
                }
            }
        }
        Files.writeString(fileListPath, fileList.joinToString("\n"))
    }
}