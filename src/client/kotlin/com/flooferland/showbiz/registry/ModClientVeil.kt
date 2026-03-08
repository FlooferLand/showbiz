package com.flooferland.showbiz.registry

import net.minecraft.client.Minecraft
import net.minecraft.core.*
import net.minecraft.util.Mth
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.utils.lerp
import foundry.veil.api.client.registry.LightTypeRegistry
import foundry.veil.api.client.render.VeilRenderSystem
import foundry.veil.api.client.render.light.data.AreaLightData
import foundry.veil.api.client.render.light.renderer.LightRenderHandle
import foundry.veil.platform.VeilEventPlatform
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import org.joml.Quaternionf

/** Veil integration for stuff like spotlights */
object ModClientVeil {
    val spotlights = hashMapOf<BlockPos, LightRenderHandle<AreaLightData>>()

    init {
        // TODO: Make it fucking rotate up and down
        //       I need a pay raise from myself
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage { stage, renderer, source, stack, fc, fc2, i, tracker, camera, frustum ->
            for ((blockPos, lightRef) in ModClientVeil.spotlights) {
                val blockEntity = Minecraft.getInstance().level?.getBlockEntity(blockPos) as? SpotlightBlockEntity ?: continue
                val facing = blockEntity.blockState.getValue(FacingEntityBlock.FACING) ?: continue
                val light = lightRef.lightData

                val pos = blockEntity.blockPos
                val forward = facing.step()
                val rotation = Quaternionf()
                    .rotationY(blockEntity.turn.x * Mth.DEG_TO_RAD)
                    .rotateX(blockEntity.turn.y * Mth.DEG_TO_RAD)
                rotation.transform(forward)

                // TODO: Make sure the speed doesn't increase/decrease on different framerates
                if (blockEntity.isOn) {
                    light.setBrightness(light.brightness + 0.005f)
                    if (light.brightness > 1f) light.setBrightness(1f)
                } else {
                    light.setBrightness(light.brightness - 0.005f)
                    if (light.brightness < 0f) light.setBrightness(0f)
                }
                light.position.set(
                    pos.x + 0.5 + (forward.x * 0.3),
                    pos.y + 0.5 + (forward.y * 0.3),
                    pos.z + 0.5 + (forward.z * 0.3)
                )
                light.orientation.set(
                    Quaternionf()
                        .rotationY(facing.toYRot() * Mth.DEG_TO_RAD)
                        .mul(rotation)
                )
            }
        }
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register { entity, level ->
            if (entity !is SpotlightBlockEntity) return@register
            ModClientVeil.spotlights[entity.blockPos]?.lightData ?: run {
                val light = AreaLightData()
                light.setColor(entity.color)
                light.setSize(0.3, 0.3)
                light.brightness = 0.0f
                light.distance = 15f
                light.angle = 45f * Mth.DEG_TO_RAD
                VeilRenderSystem.renderer().lightRenderer.addLight(light)?.let {
                    spotlights[entity.blockPos] = it
                }
            }
        }
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
            if (entity !is SpotlightBlockEntity) return@register
            val removed = spotlights.remove(entity.blockPos)
            VeilRenderSystem.renderer().lightRenderer.getLights(LightTypeRegistry.AREA.get())
                .removeIf { it.lightData.position.round() == removed?.lightData?.position?.round() }
        }
    }
}