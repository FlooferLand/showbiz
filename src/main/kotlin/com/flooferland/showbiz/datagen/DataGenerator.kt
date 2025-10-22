package com.flooferland.showbiz.datagen

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.datagen.providers.BlockProvider
import com.flooferland.showbiz.datagen.providers.ItemProvider
import com.flooferland.showbiz.datagen.providers.SoundProvider
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.registry.blocks.CustomBlockModel
import com.flooferland.showbiz.registry.blocks.CustomBlockModel.Model
import com.flooferland.showbiz.utils.Extensions.blockPath
import com.flooferland.showbiz.utils.Extensions.itemPath
import com.flooferland.showbiz.utils.rl
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import net.minecraft.*
import net.minecraft.core.registries.*
import net.minecraft.server.*
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.relativeTo

object DataGenerator {
    val engaged = runCatching { System.getProperty("$MOD_ID.datagen") }.getOrNull() == "true"

    fun registryCall(name: String) {
        val func = BuiltInRegistries::class.java.getDeclaredMethod(name)
        func.isAccessible = true
        func.invoke(null)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (!engaged) return;

        println("Running data generator..")
        SharedConstants.tryDetectVersion()
        run {  // Manual bootstrap, needed because Mojang's own bootstrap methods lock the registry
            val prop = Bootstrap::class.java.getDeclaredField("isBootstrapped")
            prop.isAccessible = true
            prop.set(null, true)

            val start = Instant.now()
            registryCall("createContents")
            Bootstrap.bootstrapDuration.set(Duration.between(start, Instant.now()).toMillis())
        }
        generate()
        registryCall("freeze")
        Bootstrap.validate()
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
            // States and models
            val builder = CustomBlockModel.BlockStateBuilder(block)
            (block.block as? CustomBlockModel)?.modelBlockStates(builder)
            if (builder.states.isNotEmpty()) {
                val stateJson = BlockProvider.generateStates(block, builder.states) ?: run {
                    print("Skipped block '${block.id}'")
                    continue
                }
                val statePath = assetsRoot / "blockstates" / "${block.id.path}.json"
                writeAsset(statePath, stateJson)

                // Write a block model for every state
                for (state in builder.states) {
                    val modelJson = BlockProvider.generateBlockModel(block, state.state.model!!) ?: continue
                    val modelPath = assetsRoot / "models" / "block" / "${state.name}.json"
                    writeAsset(modelPath, modelJson)
                }
            } else {
                // Default / empty state
                val stateJson = BlockProvider.generateStates(block, listOf()) ?: run {
                    print("Skipped block '${block.id}'")
                    continue
                }
                val statePath = assetsRoot / "blockstates" / "${block.id.path}.json"
                writeAsset(statePath, stateJson)

                // Default / empty model
                val modelJson = BlockProvider.generateBlockModel(block, Model(builder).texture(block.id)) ?: continue
                val modelPath = assetsRoot / "models" / "block" / "${block.id.path}.json"
                writeAsset(modelPath, modelJson)
            }

            // Item model
            val defaultState = builder.getDefaultState()
            val itemModelJson = BlockProvider.generateBlockItemModel(block, defaultState) ?: continue
            val itemModelPath = assetsRoot / "models" / "item" / "${block.id.path}.json"
            writeAsset(itemModelPath, itemModelJson)

            // Item (items/ entry)
            val itemJson = ItemProvider.generateItem(rl(defaultState.name.toString()).blockPath()) ?: continue
            val itemPath = assetsRoot / "items" / "${block.id.path}.json"
            writeAsset(itemPath, itemJson)
        }
        for (item in ModItems.entries) {
            // Model
            val modelJson = ItemProvider.generateModel(item) ?: run {
                println("Skipped item '${item.id}'")
                continue
            }
            val modelPath = assetsRoot / "models" / "item" / "${item.id.path}.json"
            writeAsset(modelPath, modelJson)

            // Item
            val itemJson = ItemProvider.generateItem(item.id.itemPath()) ?: continue
            val itemPath = assetsRoot / "items" / "${item.id.path}.json"
            writeAsset(itemPath, itemJson)
        }

        // Sounds
        run {
            val soundsJson = buildJsonObject {
                for (sound in ModSounds.entries) {
                    val generated = SoundProvider.generateSound(sound) ?: continue
                    put(sound.id.path, generated)
                }
            }
            val soundsPath = assetsRoot / "sounds.json"
            writeAsset(soundsPath, soundsJson)
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