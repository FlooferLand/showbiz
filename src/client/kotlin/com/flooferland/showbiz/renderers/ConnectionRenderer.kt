package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.types.ClientConnections
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.connection.ServerConnections.Point
import com.flooferland.showbiz.types.connection.ServerConnections.PointType
import com.flooferland.showbiz.utils.DrawUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.mojang.math.Axis
import kotlin.math.atan2

// Thank god for https://github.com/MrCrayfish/MrCrayfishFurnitureMod-Refurbished/tree/1.21.1
object ConnectionRenderer {
    val queued = mutableListOf<(pose: PoseStack, consumer: BufferBuilder) -> Unit>()
    val queuedText = mutableListOf<QueuedText>()

    const val POINT_POS_MUL = 0.15
    const val RENDER_DISTANCE_SQR = 32.0 * 32.0
    const val DETAIL_LABEL_DISTANCE_SQR = 10.0 * 10.0
    const val FLOW_DISTANCE_SQR = 12.0 * 12.0
    const val FLOW_SPACING = 0.2
    const val FLOW_SPEED = 0.03
    const val FLOW_MAX = 50

    val pulseColor = FastColor.ARGB32.color(90, 100, 255, 100)
    val nodeEmptyColor = FastColor.ARGB32.color(255, 150, 150, 150)
    val nodePartialColor = FastColor.ARGB32.color(255, 255, 200, 40)
    val nodeFullColor = FastColor.ARGB32.color(255, 60, 255, 90)
    val inputColor = FastColor.ARGB32.color(255, 20, 200, 255) // Blue
    val outputColor = FastColor.ARGB32.color(255, 10, 255, 30) // Green

    val flowBound = AABB.ofSize(Vec3.ZERO, 0.08, 0.08, 0.08)!!
    val pointBound = AABB.ofSize(Vec3.ZERO, 0.1, 0.1, 0.1)!!

    data class QueuedText(val x: Float, val y: Float, val z: Float, val value: Component)

    //region Rendering stuff
    fun render(player: Player, source: MultiBufferSource.BufferSource, partialTick: Float) {
        if (!isHoldingWand(player)) return
        val level = player.level() ?: return
        if (shouldShowDetail(player)) renderDetailed(player, level, partialTick) else renderSimple(player, level, partialTick)
        renderHeldConnection(player, level, partialTick)
    }

    private fun renderSimple(player: Player, level: Level, partialTick: Float) {
        val eyePos = player.getEyePosition(partialTick)
        val hoveredId = getHoveredId()
        val time = level.gameTime + partialTick.toDouble()
        for ((ownerId, points) in ClientConnections.entries) {
            val from = ownerId.grabPos(level) ?: continue
            val targets = ClientConnections.links[ownerId].orEmpty()

            val hovered = ownerId == hoveredId
            val wired = points.any { it.connected } || hovered
            if (eyePos.distanceToSqr(from) < RENDER_DISTANCE_SQR) {
                deferRender { pose, consumer ->
                    val size = if (hovered) 0.2 else 0.1
                    val bound = AABB.ofSize(Vec3.ZERO, size, size, size)
                    val nodeColor = getNodeColor(points)
                    val color = if (hovered) FastColor.ARGB32.lerp(0.25f, FastColor.ARGB32.opaque(nodeColor), CommonColors.WHITE) else nodeColor
                    renderBox(pose, consumer, from, color, bound, shadow = targets.isNotEmpty())
                }
            }

            for (targetId in targets) {
                val to = targetId.grabPos(level) ?: continue
                if (!isSegmentInRange(eyePos, from, to, RENDER_DISTANCE_SQR)) continue

                val mutual = ClientConnections.links[targetId]?.contains(ownerId) == true
                if (!(mutual && ownerId.hashCode() > targetId.hashCode())) {
                    deferRender { pose, consumer -> renderConnection(pose, consumer, from, to, outline = false) }
                }
                renderFlow(eyePos, from, to, time)
            }
        }
    }

    private fun renderDetailed(player: Player, level: Level, partialTick: Float) {
        val eyePos = player.getEyePosition(partialTick)
        for ((ownerId, points) in ClientConnections.entries) {
            val pos = ownerId.grabPos(level) ?: continue
            val near = eyePos.distanceToSqr(pos) < RENDER_DISTANCE_SQR
            val labels = player.distanceToSqr(pos) < DETAIL_LABEL_DISTANCE_SQR
            for (point in points) {
                val pointPos = pos.add(0.0, getYOffset(point.index, points.size), 0.0)
                if (near) deferRender { pose, consumer -> renderPoint(pose, consumer, point, pointPos, labels) }
                for (connection in point.connections.toList()) {
                    val target = connection.point
                    val endPos = connection.id.grabPos(level)?.add(0.0, getYOffset(target.index, target.pointCount), 0.0) ?: continue
                    if (!isSegmentInRange(eyePos, pointPos, endPos, RENDER_DISTANCE_SQR)) continue
                    deferRender { pose, consumer -> renderConnection(pose, consumer, pointPos, endPos, true) }
                }
            }
        }
    }

    private fun renderPoint(pose: PoseStack, consumer: BufferBuilder, point: Point, pos: Vec3, label: Boolean = true) {
        val color = when (point.type) {
            PointType.Input -> inputColor
            PointType.Output -> outputColor
        }

        // Rendering the box
        renderBox(pose, consumer, pos, color, pointBound, shadow = point.connected)

        // Rendering the text
        if (label) {
            val textSign = when (point.type) {
                PointType.Input -> "in"
                PointType.Output -> "out"
            }
            val text = Component.literal(textSign).withColor(color)
                .append(Component.literal(" ").withColor(0xFFFFFFFF.toInt()))
                .append(Component.literal(point.id).withColor(0xFFFFFFFF.toInt()))
            renderText(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(), text)
        }
    }

    private fun renderConnection(pose: PoseStack, consumer: BufferBuilder, startPos: Vec3, endPos: Vec3, outline: Boolean) {
        val delta = endPos.subtract(startPos)
        val length = delta.length()
        val color = FastColor.ARGB32.color(255, 100, 255, 100)
        val darkColor = FastColor.ARGB32.color(255, 10, 10, 10)
        val yaw = atan2(-delta.z, delta.x) + Mth.PI
        val pitch = atan2(delta.horizontalDistance(), delta.y) + Mth.HALF_PI

        pose.pushPose()
        pose.translate(startPos.x, startPos.y, startPos.z)
        pose.mulPose(Axis.YP.rotation(yaw.toFloat()))
        pose.mulPose(Axis.ZP.rotation(pitch.toFloat()))
        DrawUtils.drawBox(pose, consumer, AABB(0.0, -0.01125, -0.01125, length, 0.01125, 0.01125), color, alpha = 0.6f)
        if (outline) DrawUtils.drawBox(pose, consumer, AABB(0.0, -0.01125, -0.01125, length, 0.01125, 0.01125).inflate(0.01), darkColor, alpha = 0.2f)
        pose.popPose()
    }

    private fun renderFlow(eyePos: Vec3, from: Vec3, to: Vec3, time: Double) {
        val delta = to.subtract(from)
        val length = delta.length()
        if (length < 0.01) return
        if (!isSegmentInRange(eyePos, from, to, FLOW_DISTANCE_SQR)) return

        val spacing = maxOf(FLOW_SPACING, length / FLOW_MAX)
        val dir = delta.scale(1.0 / length)
        var distance = (time * FLOW_SPEED) % spacing
        while (distance < length) {
            val pos = from.add(dir.scale(distance))
            distance += spacing
            if (eyePos.distanceToSqr(pos) > FLOW_DISTANCE_SQR) continue
            deferRender { pose, consumer -> renderBox(pose, consumer, pos, pulseColor, flowBound, shadow = false) }
        }
    }

    private fun renderHeldConnection(player: Player, level: Level, partialTicks: Float) {
        val client = Minecraft.getInstance() ?: return
        val mainHand = client.options?.mainHand()?.get() ?: HumanoidArm.RIGHT
        val firstPerson = client.options?.cameraType?.isFirstPerson ?: false
        if (!firstPerson) return

        val wand = player.mainHandItem ?: return
        val heldConnection = wand.get(ModComponents.HeldConnection.type) ?: return
        val startPos = heldConnection.grabPos(level) ?: return

        val eyePos = player.getEyePosition(partialTicks)
        val view = player.getViewVector(partialTicks)
        val rightVec = view.cross(if (mainHand == HumanoidArm.RIGHT) Vec3(0.0, 1.0, 0.0) else Vec3(0.0, -1.0, 0.0)).normalize()

        val endPos = eyePos
            .add(view.scale(1.3))
            .add(rightVec.scale(1.3))

        deferRender { pose, consumer ->
            renderConnection(pose, consumer, startPos, endPos, outline = false)
            renderBox(pose, consumer, endPos, nodeFullColor, pointBound, shadow = false)
        }
    }

    private fun renderBox(pose: PoseStack, consumer: BufferBuilder, pos: Vec3, color: Int, bound: AABB, shadow: Boolean) {
        pose.pushPose()
        pose.translate(pos.x, pos.y, pos.z)
        DrawUtils.drawBox(pose, consumer, bound.inflate(-0.02), color, alpha = (FastColor.ARGB32.alpha(color).toFloat() / 255f).coerceIn(0f, 1f))
        pose.popPose()
    }

    fun renderText(x: Float, y: Float, z: Float, text: String) {
        queuedText.add(QueuedText(x, y, z, Component.literal(text)))
    }
    fun renderText(x: Float, y: Float, z: Float, text: Component) {
        queuedText.add(QueuedText(x, y, z, text))
    }

    fun renderDeferred(pose: PoseStack) {
        val mc = Minecraft.getInstance() ?: return
        val target = mc.levelRenderer?.entityTarget() ?: return
        target.bindWrite(false)

        val tesselator = Tesselator.getInstance() ?: return
        RenderSystem.enableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        )
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        pose.pushPose()

        val builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL)
        val queuedSnapshot = queued.toList()
        for (consumer in queuedSnapshot) {
            consumer(pose, builder)
        }
        builder.build()?.let { mesh -> BufferUploader.drawWithShader(mesh) }

        run { // Render text
            val mc = Minecraft.getInstance() ?: return@run
            val camera = mc.gameRenderer?.mainCamera ?: return@run
            val font = mc.font ?: return@run
            val bufferSource = mc.renderBuffers()?.bufferSource() ?: return@run

            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)

            val textSnapshot = queuedText.toList()
            for (text in textSnapshot) {
                pose.pushPose()
                pose.translate(text.x, text.y, text.z)
                pose.mulPose(camera.rotation())
                pose.mulPose(Axis.XP.rotation(Mth.PI))
                pose.scale(0.01f, 0.01f, 0.01f)
                val matrix = pose.last().pose()
                val shadowText = Component.literal(text.value.string).withColor(0xFF_00_00_00.toInt())
                font.drawInBatch(shadowText, -(font.width(shadowText) / 2).toFloat() + 1, 1f, 0xFF_00_00_00.toInt(), false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00_00_00_00,  LightTexture.FULL_BRIGHT)
                font.drawInBatch(text.value, -(font.width(text.value) / 2).toFloat(), 0f, 0xFF_FF_FF_FF.toInt(), false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0x00_00_00_00,  LightTexture.FULL_BRIGHT)
                pose.popPose()
            }
            bufferSource.endBatch()
            RenderSystem.enableDepthTest()
            RenderSystem.depthMask(true)
        }

        pose.popPose()
        RenderSystem.disableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.depthMask(true)

        mc.mainRenderTarget.bindWrite(false)

        queued.clear()
        queuedText.clear()
    }

    private fun deferRender(callback: (pose: PoseStack, consumer: BufferBuilder) -> Unit) {
        queued.add(callback)
    }
    //endregion

    //region Getter stuff
    fun getNodeColor(points: List<Point>) = when {
        points.none { it.connected } -> nodeEmptyColor
        points.all { it.connected } -> nodeFullColor
        else -> nodePartialColor
    }

    fun getYOffset(index: Int, groupSize: Int): Double {
        return (index * POINT_POS_MUL) - ((groupSize - 1) * POINT_POS_MUL / 2.0)
    }

    fun getHoveredId(): OwnerId? = when (val hit = Minecraft.getInstance()?.hitResult) {
        is BlockHitResult -> if (hit.type == HitResult.Type.BLOCK) OwnerId.of(hit.blockPos) else null
        is EntityHitResult -> OwnerId.ofEntity(hit.entity)
        else -> null
    }

    fun isHoldingWand(player: Player) = player.isHolding(ModItems.Wand.item)
    fun shouldShowDetail(player: Player) = player.isCrouching
    fun isSegmentInRange(eye: Vec3, a: Vec3, b: Vec3, maxSqr: Double): Boolean {
        val ab = b.subtract(a)
        val len = ab.lengthSqr()
        val t = if (len < 1.0E-9) 0.0 else (eye.subtract(a).dot(ab) / len).coerceIn(0.0, 1.0)
        return eye.distanceToSqr(a.add(ab.scale(t))) < maxSqr
    }
    //endregion
}