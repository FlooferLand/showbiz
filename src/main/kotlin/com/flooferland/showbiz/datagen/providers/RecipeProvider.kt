package com.flooferland.showbiz.datagen.providers

import net.minecraft.resources.*
import com.flooferland.showbiz.registry.ModRecipes
import com.flooferland.showbiz.utils.rlVanilla
import kotlinx.serialization.json.*

object RecipeProvider {
    public fun buildRecipe(recipe: ModRecipes, outId: ResourceLocation) = buildJsonObject {
        put("type", rlVanilla(recipe.data.type).toString())
        when (recipe.data) {
            is ModRecipes.ShapedRecipeData -> {
                putJsonArray("pattern") { add(recipe.data.line1); add(recipe.data.line2); add(recipe.data.line3) }
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
        putJsonObject("result") {
            put("id", outId.toString())
        }
    }
}