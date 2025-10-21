package com.flooferland.showbiz.models

import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.utils.lerp
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.*
import net.minecraft.resources.*
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedGeoModel
import kotlin.math.sin

class StagedBotBlockEntityModel : DefaultedGeoModel<StagedBotBlockEntity>(rl("conner")) {
    class CachedController(val controller: PlaybackControllerBlockEntity, val position: BlockPos?)
    var cachedController: CachedController? = null
    var lastTickTime: Double = 0.0
    val testBitmap = TestBitmap()

    override fun subtype(): String = "block"
    override fun getAnimationResource(animatable: StagedBotBlockEntity): ResourceLocation? = null

    data class MappedBit(val bitId: Byte, val drawer: Drawer, var value: Boolean = false, var valueSmooth: Float = 0f)

    @Suppress("unused")
    class TestBitmap {  // Modeled on Beach Bear
        val bits = mutableListOf<MappedBit>()

        val none = mapped(0, Drawer.Top)
        val headLeft = mapped(6, Drawer.Bottom)
        val headRight = mapped(7, Drawer.Bottom)
        val headUp = mapped(8, Drawer.Bottom)
        val body = mapped(15, Drawer.Bottom)
        val mouth = mapped(16, Drawer.Bottom)

        fun mapped(bitId: Byte, drawer: Drawer): MappedBit {
            val bit: Byte = if (drawer == Drawer.Bottom) (bitId + SignalFrame.NEXT_DRAWER).toByte() else bitId
            val mapped = MappedBit(bit, drawer)
            bits.add(mapped)
            return mapped
        }

        fun update(frame: SignalFrame, delta: Float) {
            for (bit in bits) {
                bit.value = frame.frameHas(bit.bitId)
                // if (bit.value && bit.bit != 0) println("Bit: ${bit.bit}")
                bit.valueSmooth = lerp(bit.valueSmooth, if (bit.value) 1.0f else 0.0f, 0.8f * delta)
            }
        }
    }

    override fun setCustomAnimations(animatable: StagedBotBlockEntity, instanceId: Long, state: AnimationState<StagedBotBlockEntity>) {
        // Delta-time
        val deltaTime = state.animationTick - lastTickTime
        lastTickTime = state.animationTick

        // Getting the animation controller
        if (cachedController == null || animatable.controllerPos != cachedController?.position) {
            animatable.controllerPos?.let {
                val controller = animatable.level?.getBlockEntity(it) as? PlaybackControllerBlockEntity
                if (controller != null) {
                    cachedController = CachedController(position = animatable.controllerPos, controller = controller)
                } else {
                    cachedController = null
                }
            }
        }
        if (cachedController == null) return

        // Getting bones
        val base = animationProcessor.getBone("animatronic")
        val upperBody = animationProcessor.getBone("upper_body")
        val lowerBody = animationProcessor.getBone("lower_body")
        val upperArmRight = animationProcessor.getBone("upper_arm_r")
        val lowerArmRight = animationProcessor.getBone("lower_arm_r")
        val upperArmLeft = animationProcessor.getBone("upper_arm_l")
        val lowerArmLeft = animationProcessor.getBone("lower_arm_l")
        val head = animationProcessor.getBone("head")
        val jaw = animationProcessor.getBone("jaw")

        // Reset
        upperBody.rotX = 0f
        lowerBody.rotX = 0f
        upperArmRight.rotX = 0f
        lowerArmRight.rotX = 0f
        upperArmLeft.rotX = 0f
        lowerArmLeft.rotX = 0f
        head.rotX = 0f
        jaw.rotZ = 0f

        // Base offset
        base.posY = 16.0f

        // Test animation
        val controller = cachedController!!.controller
        testBitmap.update(controller.signal, deltaTime.toFloat())
        if (controller.playing) {
            upperBody.rotX = sin(state.animationTick * 0.1f).toFloat() * 0.03f
            lowerBody.rotX = sin(state.animationTick * 0.2f).toFloat() * 0.02f
            //upperArmRight.rotX = sin(state.animationTick * 0.4f).toFloat() * 0.15f
            //lowerArmRight.rotX = sin(state.animationTick * 0.4f).toFloat() * 0.15f
            //upperArmLeft.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.15f
            //lowerArmLeft.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.15f
            head.rotX = sin(state.animationTick * 0.3f).toFloat() * 0.04f

            head.rotX += (Math.toRadians(15.0) * testBitmap.headUp.valueSmooth).toFloat()
            head.rotY = (Math.toRadians(15.0) * (testBitmap.headRight.valueSmooth - testBitmap.headLeft.valueSmooth)).toFloat()
            jaw.rotX = (Math.toRadians(-25.0) * testBitmap.mouth.valueSmooth).toFloat()
        }
    }
}