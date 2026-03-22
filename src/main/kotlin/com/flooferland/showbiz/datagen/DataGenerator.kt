package com.flooferland.showbiz.datagen

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel.Model
import com.flooferland.showbiz.datagen.providers.BlockProvider
import com.flooferland.showbiz.datagen.providers.ItemProvider
import com.flooferland.showbiz.datagen.providers.SoundProvider
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.blockPath
import com.flooferland.showbiz.utils.Extensions.itemPath
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import net.minecraft.*
import net.minecraft.core.registries.*
import net.minecraft.server.*
import net.minecraft.tags.TagKey
import net.minecraft.world.item.crafting.ShapedRecipe
import com.flooferland.showbiz.datagen.providers.LootTableProvider
import com.flooferland.showbiz.datagen.providers.RecipeProvider
import com.flooferland.showbiz.registry.ModPackets
import com.flooferland.showbiz.registry.ModRecipes
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
        run {
            // Loading things
            run {
                ModPackets
            }

            // Calling the generator
            generate()
        }
        registryCall("freeze")
        Bootstrap.validate()
    }

    fun generate() {
        val rootPath = Path.of("src", "main", "generated", "resources")
        val dataRoot = rootPath / "data" / Showbiz.MOD_ID
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

        // Recipe validation
        for (recipe in ModRecipes.entries) {
            // IDs
            for (ingredient in recipe.fetchIngredients()) {
                ingredient.item?.let { item ->
                    if (!BuiltInRegistries.ITEM.containsKey(item)) error("Recipe ingredient item '${ingredient.item}' does not exist in the Minecraft registry for recipe '${recipe.name}'")
                }
                // TODO: Add tag checking for ingredients in datagen
                /*ingredient.tag?.let { tag ->
                    val key = TagKey.create(Registries.ITEM, tag)
                    if (!BuiltInRegistries.ITEM.tags.anyMatch { it.first == key }) error("Recipe tag '${ingredient.tag}' does not exist in the Minecraft registry for recipe '${recipe.name}'")
                }*/
            }
            // Shaped
            if (recipe.data is ModRecipes.ShapedRecipeData) {
                for (char in recipe.data.let { it.line1 + it.line2 + it.line3 }) {
                    if (!recipe.data.mapping.keys.contains(char.toString()) && char != ' ') {
                        error("No idea what to map '$char' to for recipe '${recipe.name}'")
                    }
                }
            }
        }

        // Generation
        for (modBlock in ModBlocks.entries) {
            // Recipe
            if (modBlock.recipe == null && !modBlock.hideFromPlayer)
                println("Warning: No recipe provided for '${modBlock.id}'")
            else
                modBlock.recipe?.let { recipe ->
                    val recipe = RecipeProvider.buildRecipe(recipe, modBlock.id)
                    val recipePath = dataRoot / "recipe" / "${modBlock.id.path}.json"
                    writeAsset(recipePath, recipe)
                }

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
                } else println("States JSON is null for block ${modBlock.id}")

                // Write a block model for every state
                for (state in builder.states) {
                    val modelJson = BlockProvider.generateBlockModel(modBlock, state.state.model) ?: continue
                    val modelPath = assetsRoot / "models" / "block" / "${state.name}.json"
                    writeAsset(modelPath, modelJson)
                }
            } else {
                // Default / empty state
                val stateJson = BlockProvider.generateStates(modBlock, listOf())
                if (stateJson != null) {
                    val statePath = assetsRoot / "blockstates" / "${modBlock.id.path}.json"
                    writeAsset(statePath, stateJson)
                } else println("States JSON is null for block ${modBlock.id}")

                // Default / empty model
                val modelJson = BlockProvider.generateBlockModel(modBlock, Model(builder).texture(modBlock.id))
                if (modelJson != null) {
                    val modelPath = assetsRoot / "models" / "block" / "${modBlock.id.path}.json"
                    writeAsset(modelPath, modelJson)
                } else println("Model JSON is null for block ${modBlock.id}")
            }

            // Item model
            val defaultState = builder.defaultStateId
            val itemModelJson = BlockProvider.generateBlockItemModel(modBlock, defaultState.blockPath())
            if (itemModelJson != null) {
                val itemModelPath = assetsRoot / "models" / "item" / "${modBlock.id.path}.json"
                writeAsset(itemModelPath, itemModelJson)
            } else println("Item model JSON is null for block ${modBlock.id}")

            // Item ('items/' entry)
            val itemJson = ItemProvider.generateItem(defaultState.itemPath())
            if (itemJson != null) {
                val itemPath = assetsRoot / "items" / "${modBlock.id.path}.json"
                writeAsset(itemPath, itemJson)
            } else println("Item JSON is null for block ${modBlock.id}")

            // Loot table (block drops)
            val blockDrop = LootTableProvider.generateBlockDrops(modBlock)
            val lootTablePath = dataRoot / "loot_table" / "blocks" / "${modBlock.id.path}.json"
            writeAsset(lootTablePath, blockDrop)
        }
        for (modItem in ModItems.entries) {
            // Recipe
            if (modItem.recipe == null && !modItem.hideFromPlayer)
                println("Warning: No recipe provided for '${modItem.id}'")
            else
                modItem.recipe?.let { recipe ->
                    val recipe = RecipeProvider.buildRecipe(recipe, modItem.id)
                    val recipePath = dataRoot / "recipe" / "${modItem.id.path}.json"
                    writeAsset(recipePath, recipe)
                }

            // Model
            val modelJson = ItemProvider.generateModel(modItem)
            if (modelJson != null) {
                val modelPath = assetsRoot / "models" / "item" / "${modItem.id.path}.json"
                writeAsset(modelPath, modelJson)
            } else println("Model JSON is null for item ${modItem.id}")

            // Item
            val itemJson = ItemProvider.generateItem(modItem.id.itemPath())
            if (itemJson != null) {
                val itemPath = assetsRoot / "items" / "${modItem.id.path}.json"
                writeAsset(itemPath, itemJson)
            } else println("Model JSON is null for item ${modItem.id}")
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