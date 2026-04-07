package com.flooferland.showbiz.blocks.entities

import net.minecraft.ChatFormatting
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Container
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.AABB
import com.flooferland.showbiz.blocks.ReelToReelBlock.Companion.PLAYING
import com.flooferland.showbiz.blocks.entities.state.ReelToReelBlockEntityRenderState
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.network.packets.PlaybackStatePacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.show.ShowData
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.show.bitIdArrayOf
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedAudioData
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getDoubleOrNull
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getNearbyPlayers
import com.flooferland.showbiz.utils.Extensions.handItem
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import kotlin.math.roundToInt

class ReelToReelBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.ReelToReel.entityType!!, pos, blockState), IConnectable, WorldlyContainer {
    override val connectionManager = ConnectionManager(this)
    val showOut = connectionManager.port("show", PackedShowData(), PortDirection.Out)
    val audioOut = connectionManager.port("audio", PackedAudioData(), PortDirection.Out)

    val aFrameMs: Int
        get() = 50 * (if (getFormat().channels == 1) 50 * 2 else 50 * 4)
    val aBufferSize: Int
        get() = (getFormat().sampleRate.toInt() * aFrameMs) / 1_000

    public var show: ShowData = ShowData(this)
    public var seek = 0.0
    public val signal = SignalFrame()

    public var rendererState = ReelToReelBlockEntityRenderState()

    var playing = false
        public get private set
    var paused = false
        public get private set
    var hasFinished = false
        public get private set
    private val tickDelta = (50 * 0.001) // ms
    private val fps = 60  // Tied to rshw

    private var audioBytesWritten = 0

    fun tick() {
        if (!playing || show.isEmpty()) return
        var shouldUpdate = false

        seek += tickDelta
        val seekInt = (seek * fps).roundToInt()
        if (level?.isClientSide ?: true) return

        // Signal
        run {
            val entry = show.signal.getOrNull(seekInt) ?: bitIdArrayOf()
            signal.set(entry)
            shouldUpdate = signal.raw.isNotEmpty()
            showOut.send(PackedShowData(playing, signal, show.mapping))
        }

        // Audio
        val targetBytes = (seek * getFormat().sampleRate * getFormat().frameSize).toInt()
        if (audioBytesWritten < targetBytes && audioBytesWritten < show.audio.size) {
            var toWrite = aBufferSize
            toWrite -= toWrite % getFormat().frameSize

            val remain = show.audio.size - audioBytesWritten
            if (toWrite > remain) toWrite = remain

            if (toWrite > 0) {
                val chunk = show.audio.copyOfRange(audioBytesWritten, audioBytesWritten + toWrite)
                sendChunk(chunk, audioBytesWritten)
                audioBytesWritten += toWrite
            }
        }

        // Setting playing to false when the show ends
        if (audioBytesWritten >= show.audio.size - 1) {
            setPlaying(false)
            hasFinished = true
            shouldUpdate = true
        }

        // Updating the block entity (sends network packet)
        if (shouldUpdate) markDirtyNotifyAll()
    }

    private fun sendChunk(chunk: ByteArray, startIndex: Int) {
        audioOut.data.mono = chunk
        audioOut.send()
    }

    fun setPlaying(playing: Boolean) {
        this.playing = playing
        if (!playing) resetPlayback()
        showOut.send(PackedShowData(playing, signal, show.mapping))
        level?.sendBlockUpdated(blockPos, blockState, blockState.setValue(PLAYING, playing && !paused), 3)  // Visual

        // TODO: Find only near players
        val serverLevel = level as? ServerLevel ?: return
        for (player in serverLevel.players()) {
            val state = PlaybackStatePacket(blockPos, playing = playing, paused = this.paused)
            ServerPlayNetworking.send(player, state)
        }
    }

    fun setPaused(paused: Boolean) {
        this.playing = !paused
        this.paused = paused
        showOut.send(PackedShowData(playing, signal, show.mapping))
        level?.sendBlockUpdated(blockPos, blockState, blockState.setValue(PLAYING, playing && !paused), 3)  // Visual

        // TODO: Find only near players
        val serverLevel = level as? ServerLevel ?: return
        for (player in serverLevel.players()) {
            val state = PlaybackStatePacket(blockPos, playing = playing, paused = paused)
            ServerPlayNetworking.send(player, state)
        }
    }

    fun getFormat() = show.format ?: show.targetFormat

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        // Save other
        tag.putBoolean("playing", playing)
        tag.putBoolean("paused", paused)
        tag.putBoolean("finished", hasFinished)
        tag.putDouble("seek", seek)
        tag.putIntArray("signal_frame", signal.save())
        connectionManager.save(tag)
        show.saveNBT(tag)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        // Load other
        playing = tag.getBooleanOrNull("playing") ?: false
        paused = tag.getBooleanOrNull("paused") ?: false
        hasFinished = tag.getBooleanOrNull("finished") ?: false
        seek = tag.getDoubleOrNull("seek") ?: 0.0
        signal.load(tag.getIntArrayOrNull("signal_frame"))
        connectionManager.load(tag)
        show.loadNBT(tag)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)

    fun resetPlayback() {
        seek = 0.0
        audioBytesWritten = 0
        audioOut.data.chunkId = 0
        if (playing) setPlaying(false)
        hasFinished = false
        signal.reset()
    }

    // region | Container
    override fun getContainerSize() = 1
    override fun isEmpty() = show.isEmpty()
    override fun getItem(slot: Int): ItemStack {
        return show.name?.let { ReelItem.makeItem(it) } ?: ItemStack.EMPTY
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val item = show.name?.let { ReelItem.makeItem(it) }
        if (!isEmpty) {
            setPlaying(false)
            show.free()
        }
        return item ?: ItemStack.EMPTY
    }
    override fun removeItemNoUpdate(slot: Int) = removeItem(slot, 1)
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot != 0) return
        if (stack.isEmpty) {
            removeItem(slot, 1)
            return
        }
        if (stack.item is ReelItem) {
            val filename = ReelItem.getFilename(stack) ?: return
            resetPlayback()
            show.isLoaded = true
            show.load(filename) { data ->
                markDirtyNotifyAll()
                val level = level as? ServerLevel ?: return@load
                val nearby = level.getNearbyPlayers(AABB.ofSize(blockPos.center, 12.0, 12.0, 12.0))
                if (data == null) {
                    nearby.forEach {
                        it.displayClientMessage(
                            Component.translatable("message.showbiz.show_load_fail").withStyle(ChatFormatting.RED),
                            true
                        )
                    }
                    return@load
                }
                applyChange(true) {
                    setPlaying(true)
                }
            }
        }
    }
    override fun stillValid(player: Player): Boolean =
        Container.stillValidBlockEntity(this, player)
    override fun clearContent() {
        applyChange(true) {
            show.free()
            resetPlayback()
        }
    }
    override fun getSlotsForFace(side: Direction) = intArrayOf(0)
    override fun canPlaceItemThroughFace(index: Int, stack: ItemStack, direction: Direction?) =
        stack.item is ReelItem && (isEmpty || hasFinished)
    override fun canTakeItemThroughFace(index: Int, stack: ItemStack, direction: Direction) =
        !isEmpty && hasFinished && !playing
    // endregion
}