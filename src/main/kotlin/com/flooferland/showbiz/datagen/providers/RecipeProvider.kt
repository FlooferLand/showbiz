package com.flooferland.showbiz.datagen.providers

import net.minecraft.world.item.*
import com.flooferland.showbiz.registry.ModRecipes
import com.flooferland.showbiz.utils.rlVanilla
import com.mojang.serialization.JsonOps
import kotlinx.serialization.json.*

object RecipeProvider {
    public fun buildRecipe(recipe: ModRecipes, stack: ItemStack) = buildJsonObject {
        put("type", rlVanilla(recipe.data.type).toString())
        when (recipe.data) {
            is ModRecipes.ShapedRecipeData -> {
                putJsonArray("pattern") {
                    recipe.data.lines.forEach { add(it) }
                }
                putJsonObject("key") {
                    for ((key, ingredient) in recipe.data.mapping) {
                        putJsonObject(key) {
                            when {
                                ingredient.item != null -> put("item", ingredient.item.toString())
                                ingredient.tag != null -> put("tag", ingredient.tag.toString())
                            }
                        }
                    }
                }
            }
            is ModRecipes.ShapelessRecipeData -> putJsonArray("ingredients") {
                for (ingredient in recipe.fetchIngredients()) {
                    addJsonObject {
                        when {
                            ingredient.item != null -> put("item", ingredient.item.toString())
                            ingredient.tag != null -> put("tag", ingredient.tag.toString())
                        }
                    }
                }
            }
        }
        put("result",
            ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, stack).orThrow
                .let { Json.parseToJsonElement(it.toString()) }
        )
    }
}