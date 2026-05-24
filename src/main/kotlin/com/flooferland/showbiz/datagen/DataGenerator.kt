package com.flooferland.showbiz.datagen

import net.minecraft.*
import net.minecraft.core.registries.*
import net.minecraft.server.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel.Model
import com.flooferland.showbiz.datagen.providers.*
import com.flooferland.showbiz.registry.*
import com.flooferland.showbiz.utils.Extensions.blockPath
import com.flooferland.showbiz.utils.Extensions.itemPath
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
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
        run {
            // Loading things
            run {
                ModComponents.register()
                ModPackets.register()
            }

            // Calling the generator
            generate()
        }
        registryCall("freeze")
        Bootstrap.validate()
    }

    fun generate() {
        val rootPath = Path.of("..", "..", "src", "main", "resources")
        val generatedPath = Path.of("src", "main", "generated", "resources")
        val dataRoot = generatedPath / "data" / Showbiz.MOD_ID
        val assetsRoot = generatedPath / "assets" / Showbiz.MOD_ID
        val fileListPath = generatedPath / "generated.txt"
        val json = Json { prettyPrint = true }
        val fileList = mutableListOf<Path>()
        fun log(message: String, tag: String? = null) = println((tag?.let { "[ ${it} ] " } ?: "") + message)
        fun warn(message: String) = log(message, tag = "/!\\")
        fun writeAsset(path: Path, data: JsonObject?) {
            path.createParentDirectories()
            val override = (rootPath / path.relativeTo(generatedPath)).normalize()
            if (override.exists()) {
                log("Skipping '${path.relativeTo(generatedPath)}' (already exists in src/main)", tag = "-->")
                return
            }
            if (data == null) warn("JSON is null for path '$path'")

            log("Writing '${path.relativeTo(generatedPath)}'", tag = "...")
            fileList.add(path)
            Files.write(
                path.toAbsolutePath(),
                json.encodeToString(data).encodeToByteString().toByteArray()
            )
        }

        // Generation
        for (modBlock in ModBlocks.entries) {
            // States and models
            val builder = CustomBlockModel.BlockStateBuilder(modBlock)
            val block = (modBlock.block as? CustomBlockModel)
            block?.modelBlockStates(builder, modBlock.id)
            block?.modelBlockStates(builder)
            if (builder.states.isNotEmpty()) {
                val stateJson = BlockProvider.generateStates(modBlock, builder.states)
                if (stateJson != null) {
                    val statePath = assetsRoot / "blockstates" / "${modBlock.id.path}.json"
                    writeAsset(statePath, stateJson)
                } else warn("States JSON is null for block ${modBlock.id}")

                // Write a block model for every state
                for (state in builder.states) {
                    val modelJson = BlockProvider.generateBlockModel(modBlock, state.state.model) ?: continue
                    val modelPath = assetsRoot / "models" / "block" / "${state.name}.json"
                    writeAsset(modelPath, modelJson)
                }
            } else {
                // Default / empty state
                val stateJson = BlockProvider.generateStates(modBlock, listOf())
                val statePath = assetsRoot / "blockstates" / "${modBlock.id.path}.json"
                writeAsset(statePath, stateJson)

                // Default / empty model
                val modelJson = BlockProvider.generateBlockModel(modBlock, Model(builder).texture(modBlock.id))
                if (modelJson != null) {
                    val modelPath = assetsRoot / "models" / "block" / "${modBlock.id.path}.json"
                    writeAsset(modelPath, modelJson)
                } else warn("Model JSON is null for block ${modBlock.id}")
            }

            // Item model
            val defaultState = builder.defaultStateId
            val itemModelJson = BlockProvider.generateBlockItemModel(modBlock, defaultState.blockPath())
            val itemModelPath = assetsRoot / "models" / "item" / "${modBlock.id.path}.json"
            writeAsset(itemModelPath, itemModelJson)

            // Item ('items/' entry)
            val itemJson = ItemProvider.generateItem(defaultState.itemPath())
            val itemPath = assetsRoot / "items" / "${modBlock.id.path}.json"
            writeAsset(itemPath, itemJson)

            // Loot table (block drops)
            val blockDrop = LootTableProvider.generateBlockDrops(modBlock)
            val lootTablePath = dataRoot / "loot_table" / "blocks" / "${modBlock.id.path}.json"
            writeAsset(lootTablePath, blockDrop)
        }
        for (modItem in ModItems.entries) {
            // Model
            val modelJson = ItemProvider.generateModel(modItem.model!!, modItem.id)
            val modelPath = assetsRoot / "models" / "item" / "${modItem.id.path}.json"
            writeAsset(modelPath, modelJson)

            // Item
            val itemJson = ItemProvider.generateItem(modItem.id.itemPath())
            val itemPath = assetsRoot / "items" / "${modItem.id.path}.json"
            writeAsset(itemPath, itemJson)
        }
        for (musicDisc in ModMusicDiscs.entries) {
            // Model
            val modelJson = ItemProvider.generateModel(ItemProvider.ItemModelId.MusicDisc, musicDisc.id)
            val modelPath = assetsRoot / "models" / "item" / "${musicDisc.id.path}.json"
            writeAsset(modelPath, modelJson)

            // Item
            val itemJson = ItemProvider.generateItem(musicDisc.id.itemPath())
            val itemPath = assetsRoot / "items" / "${musicDisc.id.path}.json"
            writeAsset(itemPath, itemJson)

            // Song
            val songJson = JukeboxSongsProvider.generateSong(musicDisc)
            val songsPath = dataRoot / "jukebox_song" / "${musicDisc.id.path}.json"
            writeAsset(songsPath, songJson)
        }

        // Recipes
        for (modRecipe in ModRecipes.entries) {
            // Validation
            run {
                // IDs
                for (ingredient in modRecipe.fetchIngredients()) {
                    ingredient.item?.let { item ->
                        if (!BuiltInRegistries.ITEM.containsKey(item)) error("Recipe ingredient item '${ingredient.item}' does not exist in the Minecraft registry for recipe '${modRecipe.name}'")
                    }
                    // TODO: Add tag checking for ingredients in datagen
                    /*ingredient.tag?.let { tag ->
                    val key = TagKey.create(Registries.ITEM, tag)
                    if (!BuiltInRegistries.ITEM.tags.anyMatch { it.first == key }) error("Recipe tag '${ingredient.tag}' does not exist in the Minecraft registry for recipe '${recipe.name}'")
                }*/
                }
                // Shaped
                if (modRecipe.data is ModRecipes.ShapedRecipeData) {
                    for (char in modRecipe.data.let { it.line1 + it.line2 + it.line3 }) {
                        if (!modRecipe.data.mapping.keys.contains(char.toString()) && char != ' ') {
                            error("No idea what to map '$char' to for recipe '${modRecipe.name}'")
                        }
                    }
                }
            }

            // Recipe creation
            val stack = modRecipe.outputProvider()
            val itemId = BuiltInRegistries.ITEM.getKey(stack.item)
            val recipe = RecipeProvider.buildRecipe(modRecipe, stack)
            val recipePath = dataRoot / "recipe" / "${modRecipe.customId ?: itemId.path}.json"
            writeAsset(recipePath, recipe)
        }

        // Sounds
        run {
            val soundsJson = buildJsonObject {
                for (sound in ModSounds.entries) {
                    val generated = SoundProvider.generateSound(sound.sounds, sound.folder, procedural = sound.procedural) ?: continue
                    put(sound.id.path, generated)
                }
                for (disc in ModMusicDiscs.entries) {
                    val generated = SoundProvider.generateSound(arrayOf(disc.keyId), "music", stream = true) ?: continue
                    put(disc.id.path, generated)
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
                    log("Removing '$path'", "x-x")
                    val path = path.toAbsolutePath()
                    Files.deleteIfExists(path)
                }
            }
        }
        Files.writeString(fileListPath, fileList.joinToString("\n"))
    }
}