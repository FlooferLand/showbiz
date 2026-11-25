package com.flooferland.showbiz.models

import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.addons.data.BotModelData
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.types.BotError
import com.flooferland.showbiz.types.BotErrors
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlCustom
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animation.Animation
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.GeoModel

/**
 * Handles lower-level stuff, separated so it doesn't bloat up the main model file.
 */
open class BaseBotModel<T> : GeoModel<T>() where T: GeoAnimatable, T: IBot {
    protected var currentVariant: String = IBot.DEFAULT_VARIANT
    protected var currentModel: BotModelData? = null

    override fun getModelResource(animatable: T): ResourceLocation {
        if (isStrangeEvent(animatable, checkDistance = true)) return rl("geo/entity/it.geo.json")

        currentVariant = animatable.botVariant
        val botId = animatable.botId ?: run {
            BotErrors.add(BotError.MissingBot().withBot(animatable).withContext("Failed to a bot by bot ID ${animatable.botId}"))
            return errorModel
        }
        val bot = ShowbizClient.bots[botId] ?: run {
            BotErrors.add(BotError.MissingBot().withBot(animatable).withContext("Failed to get the model for bot $botId"))
            return errorModel
        }
        return bot.variants[currentVariant]?.model ?: bot.getDefaultModel()
    }

    override fun getTextureResource(animatable: T): ResourceLocation {
        if (isStrangeEvent(animatable, checkDistance = true)) return rl("textures/entity/it.png")

        val botId = animatable.botId ?: run {
            BotErrors.add(BotError.MissingBot().withBot(animatable).withContext("Failed to a bot by bot ID ${animatable.botId}"))
            return errorTexture
        }
        val bot = ShowbizClient.bots[botId] ?: run {
            BotErrors.add(BotError.MissingBot().withBot(animatable).withContext(
                "Failed to get texture for bot $botId"
            ))
            return errorTexture
        }
        return bot.variants[currentVariant]?.texture ?: bot.getDefaultTexture()
    }

    // TODO: QUICK: Make this cache the animation based on the bot ID, loading the cached version if cached
    override fun getAnimationResource(animatable: T): ResourceLocation? {
        if (isStrangeEvent(animatable, checkDistance = true)) return null
        val botId = animatable.botId ?: return null
        return ShowbizClient.bots[botId]?.variants[animatable.botVariant]?.animations
    }

    // Caching each texture's _glowmask lookup, cleared when a resource reload swaps the bot map
    private val glowTextures = HashMap<ResourceLocation, Boolean>()
    private var glowFor: Map<*, *>? = null

    public fun hasGlowTexture(animatable: T): Boolean {
        if (glowFor !== ShowbizClient.bots) {
            glowTextures.clear()
            glowFor = ShowbizClient.bots
        }
        val texture = getTextureResource(animatable)
        return glowTextures.getOrPut(texture) {
            val glowTexture = rlCustom(
                texture.namespace,
                texture.path.replace(".png", "_glowmask.png")
            )
            Minecraft.getInstance().resourceManager.getResource(glowTexture).isPresent
        }
    }

    // For some reason GeckoLib seems to require setting the active model every single time?
    override fun getBakedModel(location: ResourceLocation?): BakedGeoModel {
        if (location == null) return super.getBakedModel(errorModel)

        val variants = ShowbizClient.botModels[location]
        val model = variants?.get(currentVariant)
            ?: variants?.get(IBot.DEFAULT_VARIANT)
            ?: runCatching { return super.getBakedModel(location) }.getOrNull()
            ?: run {
                BotErrors.add(BotError.MissingModel().withContext("Failed to get any model for '${location}'"))
                return super.getBakedModel(errorModel)
            }

        if (model != currentModel) {
            currentModel = model
            animationProcessor.setActiveModel(model.bakedModel)
        }
        return model.bakedModel
    }

    override fun getAnimation(animatable: T, name: String): Animation? {
        val res = getAnimationResource(animatable) ?: run {
            BotErrors.add(BotError.MissingAnimation().withBot(animatable).withContext("Couldn't find animation file for animation '$name'"))
            null
        }
        return ShowbizClient.botAnimations[res]?.get(animatable.botVariant)?.getAnimation(name) ?: run {
            BotErrors.add(BotError.MissingAnimation().withBot(animatable).withContext("Couldn't find animation '$name' in '$res'"))
            null
        }
    }

    fun isStrangeEvent(animatable: T, checkDistance: Boolean = false): Boolean {
        val entity = animatable as? BotEntity ?: return false
        val event = animatable.strangeEvent > 0

        if (!checkDistance) return event
        val player = Minecraft.getInstance().player ?: return event
        return event && player.distanceTo(entity) > 24
    }

    companion object {
        val errorModel = rl("geo/error.geo.json")
        val errorTexture = rl("textures/error.png")
    }
}