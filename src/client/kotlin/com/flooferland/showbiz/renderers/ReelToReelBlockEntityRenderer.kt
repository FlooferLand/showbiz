package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.registry.ModClientModels
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.types.math.Color4
import com.flooferland.showbiz.utils.ClientExtensions.translateVoxel
import com.flooferland.showbiz.utils.DrawUtils
import com.flooferland.showbiz.utils.Extensions.toShortString
import com.flooferland.showbiz.utils.ShowbizEnv
import com.flooferland.showbiz.utils.lerp
import com.flooferland.showbiz.utils.rlVanilla
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.time.Duration.Companion.seconds

class ReelToReelBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ReelToReelBlockEntity> {
    override fun render(entity: ReelToReelBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val facing = entity.blockState.getValue(FacingEntityBlock.FACING) ?: return
        val minecraft = Minecraft.getInstance() ?: return
        val level = entity.level as? ClientLevel ?: return
        val reelStack = entity.getItem(0)

        // Time
        if (!minecraft.isPaused) run {
            val delta = (ShowbizClient.getDeltaTime() * 0.05).coerceIn(0.0, 1.0)
            val target = entity.seek * 200.0
            entity.clientSeek = if (!entity.showData.isLoaded) target else lerp(entity.clientSeek, target, delta)
        }
        val rotationAngle = entity.clientSeek.toFloat()

        poseStack.pushPose()
        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f))
        poseStack.translate(-0.5, 0.0, -0.5)

        // Recording
        run {
            val modelId = if (entity.recording) rlVanilla("redstone_block") else rlVanilla("gray_concrete")
            val item = if (entity.recording) Items.REDSTONE_BLOCK else Items.GRAY_CONCRETE
            val recordModel = minecraft.modelManager.getModel(ModelResourceLocation(modelId, "inventory")) ?: return@run
            poseStack.pushPose()
            poseStack.translate(0.5, 0.0, 0.5)
            poseStack.translate(0.275, 0.4, -0.15)
            poseStack.scale(0.07f, 0.07f, 0.07f)
            minecraft.itemRenderer.render(
                item.defaultInstance,
                ItemDisplayContext.NONE,
                false,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                recordModel
            )

            poseStack.pushPose()
            poseStack.translate(0.25, 0.3, -0.53)
            poseStack.scale(0.08f, -0.08f, 0.08f)
            poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            minecraft.font.drawInBatch("R", 0f, 0f, 0xFF_FF_FF_FF.toInt(), false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0x00_00_00_00, packedLight)
            poseStack.popPose()
            poseStack.popPose()
        }

        // Update indicator
        // Seeing how often the block saves/loads for dev purposes
        if (ShowbizEnv.isDev()) {
            val consumer = bufferSource.getBuffer(RenderType.gui())
            val size = 0.066
            val trackerMax = 20
            val tracker = entity.clientUpdateTracker

            poseStack.pushPose()
            poseStack.translateVoxel(0.75, 0.25, 0.7)
            poseStack.translate(0.0, 0.0, 0.0015) // Prevents clipping
            DrawUtils.drawBox(poseStack, consumer, AABB(-size, -size, 0.0, size, size, 0.0)) {
                it.color = Color4.RED.darken(tracker / trackerMax.toFloat()).pack()
                it.packedOverlay = packedOverlay
                it.packedLight = packedLight
                it.normalShade = true
                it.faces.set(Direction.SOUTH)
            }
            poseStack.popPose()

            if (tracker == 0) {
                entity.clientUpdateTracker = trackerMax
            }
        }

        // Rendering volume meter
        runCatching {
            val delta = (ShowbizClient.getDeltaTime() * 0.1).coerceIn(0.0, 1.0)
            val target = if (entity.playing && !entity.paused) entity.loudness * 3f else 0f
            if (!minecraft.isPaused) {
                entity.clientLoudness = lerp(entity.clientLoudness.toDouble(), target.toDouble(), delta).toFloat()
                entity.clientLoudness = entity.clientLoudness.coerceIn(0f, 1f)
            }

            val consumer = bufferSource.getBuffer(RenderType.solid())

            val rotation = lerp(-50.0, 50.0, entity.clientLoudness.toDouble()).toFloat()
            val width = 0.005
            val length = 0.09
            poseStack.pushPose()
            poseStack.translate(0.25, 0.14, 0.38)
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation))
            DrawUtils.drawBox(poseStack, consumer, AABB(-width, 0.0, -0.01, width, length, 0.01)) {
                it.color = CommonColors.BLACK
                it.packedOverlay = packedOverlay
                it.packedLight = packedLight
                it.normalShade = true
                it.faces.set(Direction.NORTH)
            }
            poseStack.popPose()
        }

        // Rendering the reels
        for (i in 0..1) {
            val offset = if (i == 0) 1f else -1f
            runCatching {
                var model = ModClientModels.ReelClean.getModel()
                if (model == null) model = ModClientModels.Reel.getModel()
                if (model == null) return@runCatching

                val stack = if (i == 0) reelStack else ModItems.Reel.item.defaultInstance
                if (stack.isEmpty) return@runCatching

                poseStack.pushPose()
                poseStack.translate(0.5 + (offset * 0.28), 0.715, 0.565)
                poseStack.scale(0.6f, 0.6f, 0.6f)
                poseStack.mulPose(Quaternionf().fromAxisAngleDeg(Vector3f(0f, 0f, 1f), -rotationAngle))
                poseStack.mulPose(Quaternionf().fromAxisAngleDeg(Vector3f(1f, 0f, 0f), 90.0f))
                minecraft.itemRenderer.render(
                    stack,
                    ItemDisplayContext.NONE,
                    false,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    model
                )
                poseStack.popPose()
            }.onFailure { poseStack.popPose() }
        }

        // Rendering information about the loaded reel
        if ((minecraft.player?.distanceToSqr(entity.blockPos.center) ?: 0.0) < 16.0 * 16.0) run {
            // Show name
            val showText = Component.empty()
            if (entity.showData.loading) {
                showText.append(Component.literal(entity.showData.progressText.ifEmpty { ".." }).withStyle(ChatFormatting.GRAY))
            } else if (!reelStack.isEmpty) {
                val filename = ReelItem.getFilename(reelStack) ?: reelStack.displayName.string
                val format = if (entity.showData.isLoaded) ChatFormatting.WHITE else ChatFormatting.GRAY
                if (!filename.contains('.')) {
                    showText.append(Component.literal(filename).withStyle(format))
                } else {
                    showText.append(Component.literal(filename.substringBeforeLast('.')).withStyle(format))
                    if (minecraft.player?.isCrouching ?: false) {
                        showText.append(Component.literal('.' + filename.substringAfterLast('.')).withStyle(ChatFormatting.GRAY))
                    }
                }
            }
            if (showText.string.isNotEmpty()) {
                val lines = minecraft.font.split(showText, 175)
                for ((i, line) in lines.withIndex()) {
                    val lineWidth = minecraft.font.width(line)
                    poseStack.pushPose()
                    poseStack.translate(0.5, 0.3, 0.37)
                    poseStack.scale(0.005f, -0.005f, 0.005f)
                    poseStack.mulPose(Axis.YP.rotationDegrees(180f))
                    minecraft.font.drawInBatch(
                        line,
                        -(lineWidth / 2f),
                        i.toFloat() * minecraft.font.lineHeight,
                        0xFF_FF_FF_FF.toInt(),
                        false,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0x00_00_00_00,
                        packedLight
                    )
                    poseStack.popPose()
                }
            }

            // Show length
            if (entity.showData.isLoaded) {
                val timeText = Component.literal(entity.seek.seconds.toShortString())
                    .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(entity.endTime.seconds.toShortString()))
                val lineWidth = minecraft.font.width(timeText)
                poseStack.pushPose()
                poseStack.translate(0.09, 0.36, 0.37)
                poseStack.scale(0.005f, -0.005f, 0.005f)
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
                minecraft.font.drawInBatch(
                    timeText,
                    -lineWidth.toFloat(),
                    0f,
                    0xFF_FF_FF_FF.toInt(),
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0x00_00_00_00,
                    packedLight
                )
                poseStack.popPose()
            }
        }

        poseStack.popPose()
    }
}