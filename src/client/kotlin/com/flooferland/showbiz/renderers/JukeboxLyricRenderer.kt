package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.ClientPackets
import com.flooferland.showbiz.network.packets.JukeboxLyricPacket
import com.flooferland.showbiz.utils.DrawUtils
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.JukeboxBlockEntity
import net.minecraft.world.phys.HitResult
import kotlin.math.sin

object JukeboxLyricRenderer {
    data class JukeboxData(val entity: JukeboxBlockEntity, var lyric: String)
    val jukeboxes = mutableMapOf<BlockPos, JukeboxData>()
    val playerMaxDistSqr = 16.0f.let { it * it }

    fun render(level: ClientLevel, player: LocalPlayer, buffer: MultiBufferSource.BufferSource, poseStack: PoseStack) {
        for ((blockPos, data) in jukeboxes) {
            val (entity, lyric) = data
            if (lyric.isEmpty()) continue
            if (player.distanceToSqr(blockPos.center) > playerMaxDistSqr) continue

            val text = Component.literal(lyric).withStyle(ChatFormatting.ITALIC)
            val textBlockPos = blockPos.above()
            val textWobble = sin(level.gameTime * 0.5) * 0.03f
            val textPos = textBlockPos.center.add(0.0, textWobble, 0.0)

            // Rendering lyrics as a hotbar message if not visible
            val clip = ClipContext(player.eyePosition, textPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player).let { level.clip(it) }
            if (clip.type != HitResult.Type.MISS && clip.blockPos != textBlockPos) {
                player.displayClientMessage(text, true)
                continue
            }

            // Lyric text
            DrawUtils.drawText(poseStack, textPos, text, scale = 1.5f, buffer = buffer)
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