package com.flooferland.showbiz.types

import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.forceLoad
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import com.flooferland.showbiz.utils.Extensions.secsToTicks
import com.flooferland.showbiz.utils.Extensions.toTicks
import com.flooferland.showbiz.utils.Sounds
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity

class AmbientSoundPlayer(val entity: BlockEntity, val sound: ModSounds, val block: Context.() -> Unit) {
    private val triggerCtx = TriggerContext()
    private val ctx = Context()

    private var trigger: TriggerContext.() -> Boolean = { false }
    private var start: () -> Unit = { }
    private var end: () -> Unit = { }

    var ticksUntilPlay: Long? = null

    val playing get() = ticksLeft > 0L
    var ticksLeft = 0L
    var removed = false

    init {
        val sounds = entities.getOrPut(entity) { mutableSetOf() }
        sounds += this
    }

    inner class Context() {
        fun trigger(block: TriggerContext.() -> Boolean) {
            trigger = block
        }
        fun start(block: () -> Unit) {
            start = block
        }
        fun end(block: () -> Unit) {
            end = block
        }
    }

    inner class TriggerContext() {
        fun isDark(): Boolean {
            val level = entity.level as? ServerLevel ?: return false
            return level.lightEngine.getRawBrightness(entity.blockPos, 0) < 4
        }
        fun isCaveLike(): Boolean {
            val level = entity.level as? ServerLevel ?: return false
            val blockPos = entity.blockPos ?: return false
            return (blockPos.y < -30 && level.getBlockState(blockPos.below()).`is`(Blocks.DEEPSLATE) && isDark())
        }
        fun isCave(): Boolean {
            val level = entity.level as? ServerLevel ?: return false
            val biome = level.getBiome(entity.blockPos) ?: return false
            return isCaveLike() && (
                    biome.`is`(Biomes.DRIPSTONE_CAVES)
                    || biome.`is`(Biomes.LUSH_CAVES)
                    || biome.`is`(Biomes.DEEP_DARK)
                )
        }
    }

    fun tick() {
        if (entity.isRemoved) {
            remove()
            return
        }
        val level = entity.level as? ServerLevel ?: return
        val blockPos = entity.blockPos ?: return
        block(ctx)

        // Checking playback
        val canPlay = true // trigger(triggerCtx)
        if (canPlay && ticksUntilPlay == null) {
            ticksUntilPlay = level.random.nextIntBetweenInclusive(50.secsToTicks().toInt(), 100.secsToTicks().toInt()).toLong()
        } else if (!canPlay && ticksUntilPlay != null) {
            ticksUntilPlay = null
        } else if (canPlay && ticksUntilPlay != null) {
            ticksUntilPlay = ticksUntilPlay?.let { it - 1L }
        }

        // Starting playback
        if (canPlay && (ticksUntilPlay ?: 999L) <= 0L && !playing) {
            Sounds.block(level, blockPos).play(sound)
            ticksLeft = sound.length?.toTicks() ?: 0L
            ticksUntilPlay = null
            start()
        }

        // Playing
        val oldTick = ticksLeft
        if (ticksLeft > 0L) {
            ticksLeft -= 1
        }

        // Ending
        if (oldTick > 0L && ticksLeft == 0L) {
            ticksUntilPlay = null
            end()
        }
    }

    fun remove() {
        if (ticksLeft > 0L && !removed) {
            ticksLeft = 0L
            end()
            entity.markDirtyNotifyAll()
        }
        removed = true
    }

    companion object {
        val entities = mutableMapOf<BlockEntity, MutableSet<AmbientSoundPlayer>>()

        fun register() {
            AmbientSoundPlayer::class.forceLoad()
            ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register { entity, level -> }
            ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
                val sounds = entities[entity] ?: return@register
                for (sound in sounds) sound.remove()
                entities -= entity
            }
        }
    }
}