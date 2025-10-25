package com.flooferland.showbiz.models

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.utils.lerp
import net.minecraft.resources.*
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.loading.`object`.BakedModelFactory
import software.bernie.geckolib.loading.`object`.GeometryTree
import software.bernie.geckolib.model.GeoModel
import java.lang.Math.clamp

class StagedBotBlockEntityModel : GeoModel<StagedBotBlockEntity>() {
    private var currentModel: BakedGeoModel? = null
    val bitSmooths = mutableMapOf<BitId, Double>()
    private var lastTime = System.nanoTime()

    private fun nextDelta(): Double {
        val now = System.nanoTime()
        val delta = ((now - lastTime) / 1_000_000_000.0)  // To secs (there's no function for this I checked)
            .coerceIn(0.005, 0.3)
        lastTime = now
        return delta
    }

    override fun getModelResource(animatable: StagedBotBlockEntity): ResourceLocation? {
        val botId = animatable.botId
        val model = ShowbizClient.bots[botId]?.getDefaultModel() ?: run {
            Showbiz.log.error("Failed to get model. Bot '$botId' does not exist in: [${ShowbizClient.bots.keys.joinToString(", ")}]")
            return@run null
        }
        return model
    }

    override fun getTextureResource(animatable: StagedBotBlockEntity): ResourceLocation? {
        val botId = animatable.botId
        return ShowbizClient.bots[botId]?.getDefaultTexture() ?: run {
            Showbiz.log.error("Failed to get texture. Bot '$botId' does not exist in: [${ShowbizClient.bots.keys.joinToString(", ")}]")
            return null
        }
    }

    override fun getAnimationResource(animatable: StagedBotBlockEntity): ResourceLocation? = null

    override fun getBakedModel(location: ResourceLocation?): BakedGeoModel? {
        if (location == null) error("Couldn't get baked model (null)")
        val model = ShowbizClient.models[location] ?: error("Couldn't find bot model for $location");
        val geo = GeometryTree.fromModel(model)
        val bakedModel = BakedModelFactory.getForNamespace(location.namespace).constructGeoModel(geo)
        if (bakedModel != currentModel) {
            animationProcessor.setActiveModel(bakedModel)
            currentModel = bakedModel
        }
        return currentModel
    }

    override fun setCustomAnimations(animatable: StagedBotBlockEntity, instanceId: Long, state: AnimationState<StagedBotBlockEntity>) {
        val bot = ShowbizClient.bots[animatable.botId] ?: return

        // Getting the animation controller
        // TODO: Figure out why caching the block entity doesn't work
        val controller = animatable.controllerPos?.let {
            animatable.level?.getBlockEntity(it) as? PlaybackControllerBlockEntity
        }
        if (controller == null) {
            return
        }

        // Resetting bones
        for (mapping in bot.bitmap.bits.values) {
            val rotate = mapping.rotate ?: continue
            val bone = animationProcessor.getBone(rotate.bone) ?: continue
            bone.rotX = 0f
            bone.rotY = 0f
            bone.rotZ = 0f
        }

        // Driving animation
        for ((bit, mapping) in bot.bitmap.bits) {
            // Getting things
            val rotate = mapping.rotate ?: continue
            val bone = animationProcessor.getBone(rotate.bone) ?: continue
            val frame = controller.signal
            val flowSpeed = (mapping.flow.toFloat() * 10.0f)
            val bitOn = frame.frameHas(bit)
            val delta = nextDelta()

            // Smoothing
            val oldSmooth = bitSmooths.putIfAbsent(bit, 0.0) ?: 0.0
            val bitSmooth = clamp(
                lerp(oldSmooth, if (bitOn) 1.0 else 0.0, clamp(flowSpeed * delta, 0.01, 10.0)),
                0.0, 1.0
            )
            bitSmooths[bit] = bitSmooth

            // Applying
            bone.rotX += (Math.toRadians(rotate.target.x.toDouble()) * bitSmooth).toFloat()
            bone.rotY += (Math.toRadians(rotate.target.y.toDouble()) * bitSmooth).toFloat()
            bone.rotZ += (Math.toRadians(rotate.target.z.toDouble()) * bitSmooth).toFloat()
        }
    }
}