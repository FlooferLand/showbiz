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

// TODO: Remove queuedText and make it work with normal queued rendering

// Thank god for https://github.com/MrCrayfish/MrCrayfishFurnitureMod-Refurbished/tree/1.21.1
object ConnectionRenderer {
    val queued = mutableListOf<QueuedDraw>()
    val queuedText = mutableListOf<QueuedText>()

    const val POINT_POS_MUL = 0.13
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

    enum class DrawLayer(val depthWrite: Boolean) {
        Shadow(false),
        Solid(true),
        Translucent(false)
    }
    data class QueuedText(val x: Float, val y: Float, val z: Float, val value: Component)
    data class QueuedDraw(val call: Call, val layer: DrawLayer = DrawLayer.Solid) {
        typealias Call = (pose: PoseStack, consumer: BufferBuilder) -> Unit
    }

    fun draw(player: Player, source: MultiBufferSource.BufferSource, partialTick: Float) {
        queued.clear(); queuedText.clear()
        if (!isHoldingWand(player)) return
        val level = player.level() ?: return
        if (shouldShowDetail(player)) queueRenderDetailed(player, level, partialTick) else queueRenderSimple(player, level, partialTick)
        queueHeldConnection(player, level, partialTick)
    }

    //region Queue draw utils stuff
    private fun queueRenderSimple(player: Player, level: Level, partialTick: Float) {
        val eyePos = player.getEyePosition(partialTick)
        val hoveredId = getHoveredId()
        val time = level.gameTime + partialTick.toDouble()
        for ((ownerId, points) in ClientConnections.entries) {
            val from = ownerId.grabPos(level) ?: continue
            val targets = ClientConnections.links[ownerId].orEmpty()

            val hovered = ownerId == hoveredId
            val wired = points.any { it.connected } || hovered
            if (eyePos.distanceToSqr(from) < RENDER_DISTANCE_SQR) {
                val size = if (hovered) 0.2 else 0.1
                val bound = AABB.ofSize(Vec3.ZERO, size, size, size)
                val nodeColor = getNodeColor(points)
                val color = if (hovered) FastColor.ARGB32.lerp(0.25f, FastColor.ARGB32.opaque(nodeColor), CommonColors.WHITE) else nodeColor
                queueBox(from, color, bound, shadow = wired)

                if (hovered) run {
                    val pos = ownerId.grabPos(level) ?: return@run
                    var yOffset = 0.0
                    for (point in points.distinctBy { it.id }) {
                        val pointPos = pos.add(0.0, yOffset, 0.0)
                        queueText(pointPos.x.toFloat(), pointPos.y.toFloat(), pointPos.z.toFloat(), point.id)
                        yOffset += 0.08f
                    }
                }
            }

            for (targetId in targets) {
                val to = targetId.grabPos(level) ?: continue
                if (!isSegmentInRange(eyePos, from, to, RENDER_DISTANCE_SQR)) continue

                val mutual = ClientConnections.links[targetId]?.contains(ownerId) == true
                if (!(mutual && ownerId.hashCode() > targetId.hashCode())) {
                    queueConnection(from, to, outline = false)
                }
                queueFlow(eyePos, from, to, time)
            }
        }
    }

    private fun queueRenderDetailed(player: Player, level: Level, partialTick: Float) {
        val eyePos = player.getEyePosition(partialTick)
        for ((ownerId, points) in ClientConnections.entries) {
            val pos = ownerId.grabPos(level) ?: continue
            val near = eyePos.distanceToSqr(pos) < RENDER_DISTANCE_SQR
            val labels = player.distanceToSqr(pos) < DETAIL_LABEL_DISTANCE_SQR
            for (point in points) {
                val pointPos = pos.add(0.0, getYOffset(point.index, points.size), 0.0)
                if (near) queuePoint(point, pointPos, labels)
                for (connection in point.connections.toList()) {
                    val target = connection.point
                    val endPos = connection.id.grabPos(level)?.add(0.0, getYOffset(target.index, target.pointCount), 0.0) ?: continue
                    if (!isSegmentInRange(eyePos, pointPos, endPos, RENDER_DISTANCE_SQR)) continue
                    queueConnection(pointPos, endPos, true)
                }
            }
        }
    }

    private fun queuePoint(point: Point, pos: Vec3, label: Boolean = true) {
        val color = when (point.type) {
            PointType.Input -> inputColor
            PointType.Output -> outputColor
        }

        // Rendering the box
        queueBox(pos, color, pointBound, shadow = point.connected)

        // Rendering the text
        if (label) {
            val textSign = when (point.type) {
                PointType.Input -> "in"
                PointType.Output -> "out"
            }
            val text = Component.literal(textSign).withColor(color)
                .append(Component.literal(" ").withColor(0xFFFFFFFF.toInt()))
                .append(Component.literal(point.id).withColor(0xFFFFFFFF.toInt()))
            queueText(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(), text)
        }
    }

    private fun queueFlow(eyePos: Vec3, from: Vec3, to: Vec3, time: Double) {
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
            queueBox(pos, pulseColor, flowBound, shadow = false)
        }
    }

    private fun queueHeldConnection(player: Player, level: Level, partialTicks: Float) {
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

        queueBox(endPos, nodeFullColor, pointBound, shadow = false)
        queueConnection(startPos, endPos, outline = false)
    }

    private fun queueConnection(startPos: Vec3, endPos: Vec3, outline: Boolean) {
        if (outline) deferRender(DrawLayer.Shadow) { pose, consumer ->
            renderLine(pose, consumer, startPos, endPos, 0x66000000.toInt(), thickness = 2f)
        }
        deferRender(DrawLayer.Translucent) { pose, consumer ->
            val color = FastColor.ARGB32.color(140, 100, 255, 100)
            renderLine(pose, consumer, startPos, endPos, color, thickness = 1.0f)
        }
    }

    private fun queueBox(pos: Vec3, color: Int, bound: AABB, shadow: Boolean) {
        if (shadow) {
            deferRender(DrawLayer.Shadow) { pose, consumer ->
                pose.pushPose()
                pose.translate(pos.x, pos.y, pos.z)
                DrawUtils.drawBox(pose, consumer, bound, 0xAA000000.toInt(), normalShade = false)
                pose.popPose()
            }
        }
        deferRender(if (FastColor.ARGB32.alpha(color) < 255) DrawLayer.Translucent else DrawLayer.Solid) { pose, consumer ->
            pose.pushPose()
            pose.translate(pos.x, pos.y, pos.z)
            DrawUtils.drawBox(pose, consumer, bound.inflate(-0.02), color, normalShade = true)
            pose.popPose()
        }
    }

    fun queueText(x: Float, y: Float, z: Float, text: String) {
        queueText(x, y, z, Component.literal(text))
    }
    fun queueText(x: Float, y: Float, z: Float, text: Component) {
        queuedText += QueuedText(x, y, z, text)
    }
    //endregion

    //region Render methods
    private fun renderLine(pose: PoseStack, consumer: BufferBuilder, startPos: Vec3, endPos: Vec3, color: Int, thickness: Float = 1.0f) {
        val delta = endPos.subtract(startPos)
        val length = delta.length()
        val yaw = atan2(-delta.z, delta.x) + Mth.PI
        val pitch = atan2(delta.horizontalDistance(), delta.y) + Mth.HALF_PI

        val thickness = 0.01125 * thickness
        pose.pushPose()
        pose.translate(startPos.x, startPos.y, startPos.z)
        pose.mulPose(Axis.YP.rotation(yaw.toFloat()))
        pose.mulPose(Axis.ZP.rotation(pitch.toFloat()))
        DrawUtils.drawBox(pose, consumer, AABB(0.0, -thickness, -thickness, length, thickness, thickness), color, normalShade = true)
        pose.popPose()
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

        val layers = queued.toList().groupBy { it.layer }.toSortedMap()
        for ((layer, draws) in layers) {
            RenderSystem.depthMask(layer.depthWrite)
            val builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL)
            for (draw in draws) draw.call(pose, builder)
            builder.build()?.let { mesh -> BufferUploader.drawWithShader(mesh) }
        }

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
    }

    private fun deferRender(layer: DrawLayer = DrawLayer.Solid, call: QueuedDraw.Call) {
        queued += QueuedDraw(call, layer)
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

    fun shouldShowDetail(player: Player) = player.isCrouching
    fun isHoldingWand(player: Player) = player.isHolding(ModItems.Wand.item)
    fun isSegmentInRange(eye: Vec3, a: Vec3, b: Vec3, maxSqr: Double): Boolean {
        val ab = b.subtract(a)
        val length = ab.lengthSqr()
        val factor = if (length < 1.0E-9) 0.0 else (eye.subtract(a).dot(ab) / length).coerceIn(0.0, 1.0)
        return eye.distanceToSqr(a.add(ab.scale(factor))) < maxSqr
    }
    //endregion
}