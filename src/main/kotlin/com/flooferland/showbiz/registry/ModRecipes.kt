package com.flooferland.showbiz.registry

import net.minecraft.resources.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.components.PlushComponent
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlString

enum class ModRecipes {
    StagedBot(
        "I_I",
        "I-I",
        "IRI",
        mapOf(
            "_" to Ingredient(tag="wooden_pressure_plates"),
            "I" to Ingredient("iron_ingot"),
            "-" to Ingredient("iron_bars"),
            "R" to Ingredient("redstone"),
        ),
        outputItem = { ModBlocks.StagedBot.item.defaultInstance }
    ),
    ReelToReel(
        "I I",
        "FCF",
        "XRX",
        mapOf(
            "C" to Ingredient("copper_ingot"),
            "I" to Ingredient("iron_bars"),
            "R" to Ingredient("redstone"),
            "F" to Ingredient(tag="fences"),
            "X" to Ingredient(tag="planks"),
        ),
        outputItem = { ModBlocks.ReelToReel.item.defaultInstance }
    ),
    Greybox(
        "GGG",
        "GRG",
        "GGG",
        mapOf(
            "G" to Ingredient("gray_concrete"),
            "R" to Ingredient("redstone"),
        ),
        outputItem = { ModBlocks.Greybox.item.defaultInstance }
    ),
    Speaker(
        "WIW",
        "W_W",
        "WRW",
        mapOf(
            "W" to Ingredient(tag="logs"),
            "R" to Ingredient("redstone"),
            "_" to Ingredient("dried_kelp"),
            "I" to Ingredient("iron_ingot"),
        ),
        outputItem = { ModBlocks.Speaker.item.defaultInstance }
    ),
    ShowParser(
        arrayOf(
            Ingredient("repeater"),
            Ingredient("redstone"),
            Ingredient("copper_ingot")
        ),
        outputItem = { ModBlocks.ShowParser.item.defaultInstance }
    ),
    ShowSelector(
        arrayOf(
            Ingredient(tag="buttons"),
            Ingredient("redstone"),
            Ingredient("copper_ingot")
        ),
        outputItem = { ModBlocks.ShowSelector.item.defaultInstance }
    ),
    CurtainBlock(
        "W-W",
        "W W",
        "W W",
        mapOf(
            "-" to Ingredient("chain"),
            "W" to Ingredient(tag="wool")
        ),
        outputItem = { ModBlocks.CurtainBlock.item.defaultInstance }
    ),
    CurtainControllerBlock(
        "I-I",
        "IOI",
        "I I",
        mapOf(
            "-" to Ingredient("chain"),
            "O" to Ingredient("iron_ingot"),
            "I" to Ingredient("stick")
        ),
        outputItem = { ModBlocks.CurtainController.item.defaultInstance }
    ),
    SpotlightBlock(
        arrayOf(
            Ingredient("redstone_lamp"),
            Ingredient("redstone"),
            Ingredient("copper_ingot")
        ),
        outputItem = { ModBlocks.Spotlight.item.defaultInstance }
    ),
    BitViewBlock(
        "O-O",
        "-S-",
        "ORO",
        mapOf(
            "O" to Ingredient("copper_ingot"),
            "-" to Ingredient("stick"),
            "R" to Ingredient("redstone"),
            "S" to Ingredient("amethyst_shard"),
        ),
        outputItem = { ModBlocks.BitView.item.defaultInstance }
    ),
    ProgrammerBlock(
        "IAI",
        "FCF",
        "XRX",
        mapOf(
            "A" to Ingredient("amethyst_shard"),
            "C" to Ingredient("copper_ingot"),
            "I" to Ingredient("iron_bars"),
            "R" to Ingredient("redstone"),
            "F" to Ingredient(tag="fences"),
            "X" to Ingredient(tag="planks"),
        ),
        outputItem = { ModBlocks.Programmer.item.defaultInstance }
    ),
    Wand(
        " I ",
        " IR",
        " iC",
        mapOf(
            "I" to Ingredient("chain"),
            "R" to Ingredient("redstone"),
            "i" to Ingredient("lightning_rod"),
            "C" to Ingredient("copper_ingot")
        ),
        outputItem = { ModItems.Wand.item.defaultInstance }
    ),
    Reel(
        "KKK",
        "KOK",
        "KKK",
        mapOf(
            "K" to Ingredient("dried_kelp"),
            "O" to Ingredient("iron_nugget"),
        ),
        outputItem = { ModItems.Reel.item.defaultInstance }
    ),
    EnderEarl(
        arrayOf(
            Ingredient("yellow_dye"),
            Ingredient("ender_pearl")
        ),
        outputItem = { ModItems.EnderEarl.item.defaultInstance }
    ),
    MitziPlush(
        "WWW",
        "WSW",
        "WWW",
        mapOf(
            "W" to Ingredient(tag="wool"),
            "S" to Ingredient("green_dye"),
        ),
        id = "plush_mitzi",
        outputItem = {
            ModBlocks.Plush.item.defaultInstance.apply {
                set(ModComponents.Plush.type, PlushComponent(rl("mitzi")))
            }
        }
    )
    ;

    val data: IRecipeData
    val outputProvider: () -> ItemStack
    val customId: String?

    /** Shaped recipe */
    constructor(line1: String, line2: String, line3: String, mapping: Map<String, Ingredient>, id: String? = null, outputItem: () -> ItemStack) {
        data = ShapedRecipeData(line1, line2, line3, mapping)
        outputProvider = outputItem
        customId = id
    }

    /** Shapeless recipe */
    constructor(ingredients: Array<Ingredient>, id: String? = null, outputItem: () -> ItemStack) {
        data = ShapelessRecipeData(ingredients)
        outputProvider = outputItem
        customId = id
    }

    fun fetchIngredients() = when (data) {
        is ShapedRecipeData -> data.mapping.values.toTypedArray()
        is ShapelessRecipeData -> data.ingredients
    }

    sealed interface IRecipeData {
        val type: String
    }
    data class ShapedRecipeData(val line1: String, val line2: String, val line3: String, val mapping: Map<String, Ingredient>, override val type: String = "crafting_shaped") : IRecipeData
    data class ShapelessRecipeData(val ingredients: Array<Ingredient>, override val type: String = "crafting_shapeless") : IRecipeData
    data class Ingredient(var item: ResourceLocation? = null, val tag: ResourceLocation? = null) {
        init { if (item == null && tag == null) error("No ingredient string provided") }
        constructor(block: ModBlocks) : this(item = block.id)
        constructor(item: ModItems) : this(item = item.id)
        constructor(item: String? = null, tag: String? = null) : this(
            item = item?.let { rlString(it) },
            tag = tag?.let { rlString(it) }
        )

        @Suppress("POTENTIALLY_NON_REPORTED_ANNOTATION")
        @Deprecated("Unimplemented")
        override fun toString() = ""
    }
}