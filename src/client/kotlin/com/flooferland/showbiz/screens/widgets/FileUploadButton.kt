package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.*
import net.minecraft.sounds.SoundEvents
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.network.packets.FileUploadChunkPacket
import com.flooferland.showbiz.network.packets.FileUploadHeaderPacket
import com.flooferland.showbiz.network.packets.FileUploadResponsePacket
import com.flooferland.showbiz.network.packets.FileUploadResponsePacket.ServerMessage
import com.mojang.blaze3d.systems.RenderSystem
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyfd.TinyFileDialogs
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.name
import kotlin.math.max
import kotlin.math.roundToInt

class FileUploadButton(x: Int, y: Int, width: Int = 200, val filter: Filter? = null) : AbstractButton(x, y, max(width, 100), 40, Component.literal("Choose a file")) {
    public var title = "Select a file"
    public var onSubmit: (Path?) -> Unit = { _ -> }
    public var onUploadFinish: () -> Unit = { }

    public var loadedPath: Path? = null

    var tooltipText = Component.empty()!!

    public var isUploadFinished = false
    val innerHeight get() = (Minecraft.getInstance()?.font?.lineHeight ?: 100) + 10
    val hasFile get() = loadedPath != null

    var processing = false
    var fileSizeBytes = 0L
    var progressBytes = 0L
    var fileStream: InputStream? = null

    init {
        update()
        ClientPlayNetworking.registerReceiver(FileUploadResponsePacket.type) { packet, context -> context.client().execute {
            val fileStream = fileStream
            if (packet.status == ServerMessage.FuckOff || fileStream == null) {
                reset()
                context.client().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.3f))
                return@execute
            }
            if (packet.status == ServerMessage.Done) {
                fileStream.close()
                processing = false
                isUploadFinished = true
                progressBytes = 0L
                update()
                context.client().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.0f))
                return@execute
            }
            progressBytes = packet.bytesSoFar
            val chunkSize = 128 * 1024
            val packet = FileUploadChunkPacket(runCatching { fileStream.readNBytes(chunkSize) }.getOrNull() ?: byteArrayOf())
            ClientPlayNetworking.send(packet)
        } }
    }

    fun update() {
        val font = Minecraft.getInstance().font
        message = Component.literal(loadedPath?.name ?: "Choose a file")
        tooltipText = Component.literal(loadedPath?.let { "Right-click to reset" } ?: "Click to upload a file")

        loadedPath?.let { loadedPath ->
            val maxWidth = width - font.width(".. .") - 5
            if (font.width(message) > maxWidth) run {
                val new = font.split(message, maxWidth).firstOrNull() ?: return@run
                val newString = StringBuilder().also { new.accept { _, _, c -> it.appendCodePoint(c); true } }
                message = Component.literal("$newString.. .${loadedPath.extension}")
            }
        }
        onSubmit(loadedPath)
        if (isUploadFinished) onUploadFinish()
    }

    fun submit(path: Path) {
        fileSizeBytes = runCatching { path.fileSize() }.getOrNull() ?: 0L
        if (fileSizeBytes > 0L) {
            fileStream = Files.newInputStream(path)
            ClientPlayNetworking.send(FileUploadHeaderPacket(path.name, fileSizeBytes))
            processing = true
        } else Showbiz.log.error("Failed to send audio to the server. File size is 0")
    }

    fun reset() {
        fileStream?.close()
        fileStream = null
        fileSizeBytes = 0
        progressBytes = 0
        processing = false
        isUploadFinished = false
        loadedPath = null
        update()
    }

    override fun onPress() {
        val filters = filter?.let {
            val pointer = MemoryUtil.memAllocPointer(1)
            pointer.put(MemoryUtil.memUTF8("*.${it.ext}")).flip()
            pointer
        }
        val path = TinyFileDialogs.tinyfd_openFileDialog(
            title,
            "",
            filters, filter?.title,
            false
        )
        MemoryUtil.memFree(filters)
        path?.let { first ->
            loadedPath = runCatching { Path.of(first) }.getOrNull()
            if (filter != null && loadedPath?.extension != filter.ext) {
                Minecraft.getInstance()?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.3f))
                reset()
            }
            loadedPath?.let { submit(it); onSubmit(it) } ?: onSubmit(null)
        }
        update()
    }

    override fun clicked(mouseX: Double, mouseY: Double) =
        super.clicked(mouseX, mouseY) && (mouseY < y+innerHeight) && !processing

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && clicked(mouseX, mouseY)) {
            playDownSound(Minecraft.getInstance().getSoundManager())
            reset()
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val minecraft = Minecraft.getInstance()
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, this.alpha)
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()

        val hovering = (mouseX > x && mouseX < x+width) && (mouseY > y && mouseY < y+innerHeight)
        val color = if (hovering) 0xFF555555.toInt() else 0xFF444444.toInt()

        // Outer plate
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 0.3f)
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000.toInt())
        guiGraphics.fill(x, y, x + width, y + height, 0xAA2222222.toInt())

        // Inner name plate
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, this.alpha)
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + innerHeight + 1, 0xFF000000.toInt())
        guiGraphics.fill(x, y, x + width, y + innerHeight, color)

        // Progress bar
        var progressText: String? = null
        if (processing) {
            var pad = 4
            val maxBarWidth = width - (pad * 2)
            val progress = (progressBytes.toFloat() / fileSizeBytes.coerceAtLeast(1L).toFloat())
            val barRight = (x + pad + (maxBarWidth * progress).roundToInt()).coerceAtMost(x + width)
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 0.7f)
            guiGraphics.fill(x + pad, (y + innerHeight) + pad, barRight, (y + height) - pad, 0xFF000000.toInt())
            pad += 1
            guiGraphics.fill(x + pad, (y + innerHeight) + pad, barRight, (y + height) - pad, 0xFF44FF44.toInt())
            progressText = "${(progress * 100f).roundToInt()}%"
        } else if (isUploadFinished) {
            progressText = "Upload finished!"
        }
        progressText?.let { guiGraphics.drawString(minecraft.font, it, x + 5, (y + innerHeight) + 5, 0xFFFFFFFF.toInt()) }

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f)
        guiGraphics.drawString(minecraft.font, message, x + 5, y + ((innerHeight - 6) / 2), if (loadedPath != null) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())

        if (hovering) guiGraphics.renderTooltip(minecraft.font, tooltipText, mouseX, mouseY)
    }

    override fun updateWidgetNarration(narration: NarrationElementOutput) {
        narration.add(NarratedElementType.HINT, "File upload button")
    }

    enum class Filter(val ext: String, val title: String) {
        AudioWav("wav", "WAV File")
    }

    companion object {
        init {
            ScreenEvents.AFTER_INIT.register { minecraft, screen, i, i1 ->
                ScreenEvents.remove(screen).register { closedScreen ->
                    for (widget in closedScreen.children()) {
                        if (widget is FileUploadButton) widget.fileStream?.close()
                    }
                    if (minecraft.currentServer != null) runCatching { ClientPlayNetworking.unregisterReceiver(FileUploadResponsePacket.type.id) }
                }
            }
        }
    }
}