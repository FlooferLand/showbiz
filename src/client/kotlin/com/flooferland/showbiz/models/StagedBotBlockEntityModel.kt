package com.flooferland.showbiz.models

import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.utils.lerp
import com.flooferland.showbiz.utils.rl
import net.minecraft.resources.*
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import java.lang.Math.clamp

class StagedBotBlockEntityModel : DefaultedGeoModel<StagedBotBlockEntity>(rl("conner")) {
    val testBitmap = TestBitmap()

    override fun getTextureResource(animatable: StagedBotBlockEntity, renderer: GeoRenderer<StagedBotBlockEntity>?): ResourceLocation? {
        val entity = renderer?.animatable ?: return super.getTextureResource(animatable, renderer)
        return when (entity.modelId) {
            1 -> rl("textures/block/conner_mitzi.png")
            else -> super.getTextureResource(animatable, renderer)
        }
    }

    override fun subtype(): String = "block"
    override fun getAnimationResource(animatable: StagedBotBlockEntity): ResourceLocation? = null

    data class MappedBit(val bitId: Byte, val drawer: Drawer, val flowSpeed: Double, var value: Boolean = false, var valueSmooth: Double = 0.0)

    @Suppress("unused")
    class TestBitmap {  // Modeled on Beach Bear
        val bits = mutableListOf<MappedBit>()

        val none = mapped(0, Drawer.Top, 1.0)
        val headLeft = mapped(6, Drawer.Bottom, 1.0)
        val headRight = mapped(7, Drawer.Bottom, 1.0)
        val headUp = mapped(8, Drawer.Bottom, 1.0)
        val body = mapped(15, Drawer.Bottom, 0.3)
        val mouth = mapped(16, Drawer.Bottom, 1.5)
        val mouth2 = mapped(35, Drawer.Bottom, 1.5)

        fun mapped(bitId: Byte, drawer: Drawer, flowSpeed: Double): MappedBit {
            val bit: Byte = if (drawer == Drawer.Bottom) (bitId + SignalFrame.NEXT_DRAWER).toByte() else bitId
            val mapped = MappedBit(bit, drawer, flowSpeed)
            bits.add(mapped)
            return mapped
        }

        fun update(frame: SignalFrame) {
            val delta = nextDelta()

            for (bit in bits) {
                val flowSpeed = (bit.flowSpeed.toFloat() * 3.5f)
                bit.value = frame.frameHas(bit.bitId)
                bit.valueSmooth = clamp(
                    lerp(bit.valueSmooth, if (bit.value) 1.0 else 0.0, clamp(flowSpeed * delta, 0.01, 10.0)),
                    0.0, 1.0
                )
            }
        }

        private var lastTime = System.nanoTime()
        private fun nextDelta(): Double {
            val now = System.nanoTime()
            val delta = ((now - lastTime) / 1_000_000_000.0)  // To secs (there's no function for this I checked)
                        .coerceIn(0.005, 0.3)
            lastTime = now
            return delta
        }
    }

    override fun setCustomAnimations(animatable: StagedBotBlockEntity, instanceId: Long, state: AnimationState<StagedBotBlockEntity>) {
        // Getting bones
        val base = animationProcessor.getBone("animatronic")
        val upperBody = animationProcessor.getBone("upper_body")
        val lowerBody = animationProcessor.getBone("lower_body")
        val head = animationProcessor.getBone("head")
        val jaw = animationProcessor.getBone("jaw")

        // Base offset
        base.posY = 16.0f

        // Getting the animation controller
        // TODO: Figure out why caching the block entity doesn't work
        val controller = animatable.controllerPos?.let {
            animatable.level?.getBlockEntity(it) as? PlaybackControllerBlockEntity
        }
        if (controller == null) {
            return
        }

        // Test animation
        testBitmap.update(controller.signal)
        if (controller.playing) {
            upperBody.rotX = (Math.toRadians(-4.0) * testBitmap.body.valueSmooth).toFloat()
            lowerBody.rotX = (Math.toRadians(-6.0) * testBitmap.body.valueSmooth).toFloat()

            head.rotX += (Math.toRadians(15.0) * testBitmap.headUp.valueSmooth).toFloat()
            head.rotY = (Math.toRadians(15.0) * (testBitmap.headRight.valueSmooth - testBitmap.headLeft.valueSmooth)).toFloat()

            val mouth = if (animatable.modelId == 0) testBitmap.mouth else testBitmap.mouth2
            jaw.rotX = (Math.toRadians(-25.0) * mouth.valueSmooth).toFloat()
        }
    }
}