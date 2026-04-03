package com.flooferland.showbiz.registry

import net.minecraft.resources.*
import com.flooferland.showbiz.utils.rlString

enum class ModRecipes {
    StagedBot(
        "X_X",
        "XIX",
        "XRX",
        mapOf(
            "_" to Ingredient(tag="wooden_pressure_plates"),
            "X" to Ingredient("iron_block"),
            "I" to Ingredient("iron_bars"),
            "R" to Ingredient("redstone"),
        )
    ),
    ReelToReel(
        " C ",
        "IRI",
        " X ",
        mapOf(
            "C" to Ingredient("copper_ingot"),
            "I" to Ingredient("iron_bars"),
            "R" to Ingredient("redstone"),
            "X" to Ingredient("iron_block"),
        )
    ),
    Greybox(
        "GGG",
        "GRG",
        "GGG",
        mapOf(
            "G" to Ingredient("gray_concrete"),
            "R" to Ingredient("redstone"),
        )
    ),
    Speaker(
        "WWW",
        "WRW",
        "WIW",
        mapOf(
            "W" to Ingredient("oak_log"),
            "R" to Ingredient("redstone"),
            "I" to Ingredient("iron_ingot"),
        )
    ),
    ShowParser(
        arrayOf(
            Ingredient("repeater"),
            Ingredient("redstone"),
            Ingredient("copper_ingot")
        )
    ),
    ShowSelector(
        arrayOf(
            Ingredient(tag="buttons"),
            Ingredient("redstone"),
            Ingredient("copper_ingot")
        )
    ),
    CurtainBlock(
        "W-W",
        "W W",
        "W W",
        mapOf(
            "-" to Ingredient("chain"),
            "W" to Ingredient(tag="wool")
        )
    ),
    CurtainControllerBlock(
        "I-I",
        "IOI",
        "I I",
        mapOf(
            "-" to Ingredient("chain"),
            "O" to Ingredient("iron_ingot"),
            "I" to Ingredient("stick")
        )
    ),
    SpotlightBlock(
        arrayOf(
            Ingredient("redstone_lamp"),
            Ingredient("redstone"),
            Ingredient("copper_ingot")
        )
    ),
    Wand(
        " I ",
        " I ",
        "RiC",
        mapOf(
            "I" to Ingredient("iron_bars"),
            "R" to Ingredient("redstone"),
            "i" to Ingredient("lightning_rod"),
            "C" to Ingredient("copper_ingot")
        )
    ),
    Reel(
        "KKK",
        "KFK",
        "KKK",
        mapOf(
            "K" to Ingredient("dried_kelp"),
            "F" to Ingredient(tag="fences"),
        )
    ),
    ;

    val data: IRecipeData

    /** Shaped recipe */
    constructor(line1: String, line2: String, line3: String, mapping: Map<String, Ingredient>) {
        data = ShapedRecipeData(line1, line2, line3, mapping)
    }

    /** Shapeless recipe */
    constructor(ingredients: Array<Ingredient>) {
        data = ShapelessRecipeData(ingredients)
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