package com.flooferland.showbiz.models

import net.minecraft.client.*
import net.minecraft.resources.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.addons.data.BotModelData
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlCustom
import software.bernie.geckolib.animation.Animation
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.GeoModel

/**
 * Handles lower-level stuff, separated so it doesn't bloat up the main model file.
 */
open class BaseBotModel : GeoModel<StagedBotBlockEntity>() {
    protected var currentModel: BotModelData? = null

    enum class Error {
        MissingModel,
        MissingTexture,
        RenderException;
        var context: String? = null
        var botId: String? = null
        fun withContext(context: String): Error {
            this.context = context
            return this
        }
        fun withBot(animatable: StagedBotBlockEntity): Error {
            this.botId = animatable.botId.toString()
            return this
        }
    }

    override fun getModelResource(animatable: StagedBotBlockEntity): ResourceLocation {
        val botId = animatable.botId ?: return emptyModel
        val model = ShowbizClient.bots[botId]?.getDefaultModel() ?: run {
            if (!errorsTriggered.contains(Error.MissingModel)) {
                errorsTriggered.add(
                    Error.MissingModel.withBot(animatable).withContext(
                        "Failed to get model. Bot '$botId' does not exist in: [${ShowbizClient.bots.keys.joinToString(", ")}]. This error usually occurs when you haven't added a bot correctly"
                    )
                )
            }
            emptyModel
        }
        return model
    }

    override fun getTextureResource(animatable: StagedBotBlockEntity): ResourceLocation {
        val botId = animatable.botId ?: return emptyTexture
        return ShowbizClient.bots[botId]?.getDefaultTexture() ?: run {
            if (!errorsTriggered.contains(Error.MissingTexture)) {
                errorsTriggered.add(
                    Error.MissingTexture.withBot(animatable).withContext(
                        "Failed to get texture. Bot '$botId' does not exist in: [${ShowbizClient.bots.keys.joinToString(", ")}]"
                    )
                )
            }
            emptyModel
        }
    }

    // TODO: QUICK: Make this cache the animation based on the bot ID, loading the cached version if cached
    override fun getAnimationResource(animatable: StagedBotBlockEntity): ResourceLocation? {
        val botId = animatable.botId ?: return null
        ShowbizClient.bots[botId]?.let { bot ->
            bot.animations?.let { anims -> return anims }
        }
        return null
    }

    fun hasGlowTexture(animatable: StagedBotBlockEntity): Boolean {
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

    override fun getAnimation(animatable: StagedBotBlockEntity, name: String): Animation? {
        val res = getAnimationResource(animatable) ?: run {
            Showbiz.log.error("Couldn't find animation file for animation '$name' (bot=${animatable.botId})")
            null
        }
        return ShowbizClient.animations[res]?.getAnimation(name) ?: run {
            Showbiz.log.error("Couldn't find animation '$name' in '$res' (bot=${animatable.botId})")
            null
        }
    }

    companion object {
        val errorsTriggered = mutableSetOf<Error>()
        val emptyModel = rl("geo/entity/empty.geo.json")
        val emptyTexture = rl("textures/empty.png")
    }
}