package com.flooferland.showbiz.renderers

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.multiplayer.*
import net.minecraft.client.player.*
import net.minecraft.client.renderer.*
import net.minecraft.core.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.ClientPackets
import com.flooferland.showbiz.network.packets.JukeboxLyricPacket
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object JukeboxLyricRenderer {
    data class JukeboxData(val entity: JukeboxBlockEntity, var lyric: String)
    val jukeboxes = mutableMapOf<BlockPos, JukeboxData>()
    val playerMaxDistSqr = 16.0f.let { it * it }

    fun render(level: ClientLevel, player: LocalPlayer, buffer: MultiBufferSource.BufferSource, pose: PoseStack) {
        val minecraft = Minecraft.getInstance() ?: return
        val font = minecraft.font ?: return
        val camera = minecraft.gameRenderer?.mainCamera ?: return

        for ((blockPos, data) in jukeboxes) {
            val (entity, lyric) = data
            if (lyric.isEmpty()) continue
            if (player.distanceToSqr(blockPos.center) > playerMaxDistSqr) continue

            val text = Component.literal(lyric).withStyle(ChatFormatting.ITALIC)
            val textBlockPos = blockPos.above()
            val textPos = textBlockPos.center

            // Rendering lyrics as a hotbar message if not visible
            val clip = ClipContext(player.eyePosition, textPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player).let { level.clip(it) }
            if (clip.type != HitResult.Type.MISS && clip.blockPos != textBlockPos) {
                player.displayClientMessage(text, true)
                continue
            }

            // Lyric text
            pose.pushPose()
            pose.translate(textPos.x, textPos.y, textPos.z)
            pose.mulPose(camera.rotation())
            pose.mulPose(Axis.XP.rotation(Mth.PI))
            pose.scale(0.015f, 0.015f, 0.015f)
            val matrix = pose.last().pose()
            val maxWidth = 130
            val lines = font.split(text, maxWidth)
            var yOffset = 0f
            for (line in lines) {
                val lineWidth = font.width(line)
                val xOffset = -lineWidth / 2f

                val padding = 2
                val backgroundColor = 0x80_00_00_00.toInt()
                val background = buffer.getBuffer(RenderType.textBackground())
                background.addVertex(matrix, xOffset - padding, yOffset - padding, 0f).setColor(backgroundColor).setLight(LightTexture.FULL_BRIGHT)
                background.addVertex(matrix, xOffset - padding, yOffset + font.lineHeight + padding, 0f).setColor(backgroundColor).setLight(LightTexture.FULL_BRIGHT)
                background.addVertex(matrix, xOffset + lineWidth + padding, yOffset + font.lineHeight + padding, 0f).setColor(backgroundColor).setLight(LightTexture.FULL_BRIGHT)
                background.addVertex(matrix, xOffset + lineWidth + padding, yOffset - padding, 0f).setColor(backgroundColor).setLight(LightTexture.FULL_BRIGHT)
                font.drawInBatch(line, xOffset, yOffset, 0xFF_FF_FF_FF.toInt(), false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0,  LightTexture.FULL_BRIGHT)

                yOffset += font.lineHeight + 4
            }
            pose.popPose()
        }
    }

    fun register() {
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register { blockEntity, level ->
            if (blockEntity is JukeboxBlockEntity)
                jukeboxes[blockEntity.blockPos] = JukeboxData(blockEntity, "")
        }
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { blockEntity, level ->
            if (jukeboxes.containsKey(blockEntity.blockPos))
                jukeboxes.remove(blockEntity.blockPos)
        }
        ClientTickEvents.END_WORLD_TICK.register { level ->
            jukeboxes.entries.removeIf { (blockPos, data) ->
                val newEntity = level.getBlockEntity(blockPos)
                newEntity !is JukeboxBlockEntity || data.entity.isRemoved
            }
        }
        ClientPackets.listen(JukeboxLyricPacket.type) { packet, ctx ->
            val level = ctx.player()?.clientLevel ?: return@listen
            val data = jukeboxes.getOrPut(packet.blockPos) {
                val entity = level.getBlockEntity(packet.blockPos) as? JukeboxBlockEntity ?: return@listen
                JukeboxData(entity, packet.lyric)
            }
            data.lyric = packet.lyric
            jukeboxes[packet.blockPos] = data
        }
    }
}