package com.flooferland.showbiz.models

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import net.minecraft.resources.*
import software.bernie.geckolib.animation.Animation
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.GeoModel

/**
 * Handles lower-level stuff, separated so it doesn't bloat up the main model file.
 */
open class BaseBotModel : GeoModel<StagedBotBlockEntity>() {
    var currentModel: BakedGeoModel? = null

    override fun getModelResource(animatable: StagedBotBlockEntity): ResourceLocation {
        val botId = animatable.botId
        val model = ShowbizClient.bots[botId]?.getDefaultModel() ?: run {
            error("Failed to get model. Bot '$botId' does not exist in: [${ShowbizClient.bots.keys.joinToString(", ")}]. This error usually occurs when you haven't added a bot correctly")
        }
        return model
    }

    override fun getTextureResource(animatable: StagedBotBlockEntity): ResourceLocation {
        val botId = animatable.botId
        return ShowbizClient.bots[botId]?.getDefaultTexture() ?: run {
            error("Failed to get texture. Bot '$botId' does not exist in: [${ShowbizClient.bots.keys.joinToString(", ")}]")
        }
    }

    // TODO: QUICK: Make this cache the animation based on the bot ID, loading the cached version if cached
    override fun getAnimationResource(animatable: StagedBotBlockEntity): ResourceLocation? {
        val botId = animatable.botId
        ShowbizClient.bots[botId]?.let { bot ->
            bot.animations?.let { anims -> return anims }
        }
        return null
    }

    override fun getBakedModel(location: ResourceLocation?): BakedGeoModel? {
        if (location == null) error("Couldn't get baked model (null)")
        val bakedModel = ShowbizClient.models[location] ?: error("Couldn't find bot model for $location");
        if (bakedModel != currentModel) {
            animationProcessor.setActiveModel(bakedModel)
            currentModel = bakedModel
        }
        return currentModel
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
}