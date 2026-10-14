package com.flooferland.showbiz

import net.minecraft.data.*
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.datagen.providers.LootTableProvider
import com.flooferland.showbiz.datagen.providers.ModelProvider
import java.util.concurrent.CompletableFuture
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class ShowbizDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
        val pack = generator.createPack()
        pack.addProvider(::ModelProvider)
        pack.addProvider(::LootTableProvider)
        pack.addProvider { packOutput ->
            object : DataProvider {
                override fun run(output: CachedOutput): CompletableFuture<*>? {
                    // TODO: Add checking recipes back in (in DataGenerator.main)
                    System.setProperty("$MOD_ID.datagen", "true")
                    DataGenerator.generate(output, packOutput.outputFolder)
                    DataGenerator.check(packOutput.outputFolder)
                    return CompletableFuture.completedFuture(null)
                }
                override fun getName() = "Showbiz custom"
            }
        }
    }
}