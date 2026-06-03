package com.flooferland.showbiz.models

import net.minecraft.client.*
import net.minecraft.resources.*
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.addons.data.BotModelData
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlCustom
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animation.Animation
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.GeoModel

/**
 * Handles lower-level stuff, separated so it doesn't bloat up the main model file.
 */
open class BaseBotModel<T> : GeoModel<T>() where T: GeoAnimatable, T: IBot {
    protected var currentModel: BotModelData? = null

    enum class Error {
        MissingBot,
        MissingModel,
        MissingTexture,
        MissingAnimation,
        RenderException;
        var context: String? = null
        var botId: String? = null
        fun withContext(context: String): Error {
            this.context = context
            return this
        }
        fun withBot(animatable: IBot): Error {
            this.botId = animatable.botId.toString()
            return this
        }
    }

    override fun getModelResource(animatable: T): ResourceLocation {
        val botId = animatable.botId ?: return emptyModel
        val bot = ShowbizClient.bots[botId] ?: run {
            errorsTriggered += Error.MissingBot.withBot(animatable).withContext(
                "Failed to get model. The bot '$botId' does not exist in: [${ShowbizClient.bots.keys.joinToString(", ")}]. This error usually occurs when you haven't added a bot correctly"
            )
            return emptyModel
        }
        return bot.getDefaultModel()
    }

    override fun getTextureResource(animatable: T): ResourceLocation {
        val botId = animatable.botId ?: return emptyTexture
        val bot = ShowbizClient.bots[botId] ?: run {
            errorsTriggered += Error.MissingBot.withBot(animatable).withContext(
                "Failed to get texture. The bot '$botId' does not exist in: [${ShowbizClient.bots.keys.joinToString(", ")}]"
            )
            return emptyTexture
        }
        return bot.getDefaultTexture()
    }

    // TODO: QUICK: Make this cache the animation based on the bot ID, loading the cached version if cached
    override fun getAnimationResource(animatable: T): ResourceLocation? {
        val botId = animatable.botId ?: return null
        ShowbizClient.bots[botId]?.let { bot ->
            bot.animations?.let { anims -> return anims }
        }
        return null
    }

    public fun hasGlowTexture(animatable: T): Boolean {
        val texture = getTextureResource(animatable)
        val glowTexture = rlCustom(
            texture.namespace,
            texture.path.replace(".png", "_glowmask.png")
        )
        return Minecraft.getInstance().resourceManager.getResource(glowTexture).isPresent
    }

    // For some reason GeckoLib seems to require setting the active model every single time?
    override fun getBakedModel(location: ResourceLocation?): BakedGeoModel {
        if (location == null) {
            errorsTriggered.add(Error.MissingModel.withContext("Bot model failed to bake"))
            return super.getBakedModel(emptyModel)
        }
        val model = ShowbizClient.botModels[location] ?: run {
            errorsTriggered.add(Error.MissingModel.withContext("Bot model failed to bake"))
            return super.getBakedModel(emptyModel)
        }

        if (model != currentModel) {
            currentModel = model
            animationProcessor.setActiveModel(model.bakedModel)
        }
        return model.bakedModel
    }

    override fun getAnimation(animatable: T, name: String): Animation? {
        val res = getAnimationResource(animatable) ?: run {
            errorsTriggered.add(Error.MissingAnimation.withContext("Couldn't find animation file for animation '$name'"))
            null
        }
        return ShowbizClient.animations[res]?.getAnimation(name) ?: run {
            errorsTriggered.add(Error.MissingAnimation.withContext("Couldn't find animation '$name' in '$res'"))
            null
        }
    }

    companion object {
        val errorsTriggered = mutableSetOf<Error>()
        val emptyModel = rl("geo/empty.geo.json")
        val emptyTexture = rl("textures/empty.png")
    }
}