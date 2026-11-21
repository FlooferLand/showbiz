package com.flooferland.showbiz.types

import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.forceLoad
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import com.flooferland.showbiz.utils.Extensions.toTicks
import com.flooferland.showbiz.utils.Sounds
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import kotlin.time.Duration

class AmbientEventPlayer(val entity: BlockEntity, val sound: ModSounds, val block: Context.() -> Unit) {
    private val triggerCtx = TriggerContext()
    private val ctx = Context()

    private var every: Pair<Duration, Duration> = Pair(Duration.INFINITE, Duration.INFINITE)
    private var trigger: TriggerContext.() -> Boolean = { false }
    private var start: () -> Unit = { }
    private var update: () -> Unit = { }
    private var end: () -> Unit = { }

    var canPlay: Boolean = false
    var ticksUntilPlay: Long? = null

    val playing get() = ticksLeft > 0L
    var ticksLeft = 0L
    var removed = false

    init {
        val sounds = entities.getOrPut(entity) { mutableSetOf() }
        sounds += this
    }

    inner class Context() {
        fun every(time: Pair<Duration, Duration>) { every = time }
        fun trigger(block: TriggerContext.() -> Boolean) { trigger = block }
        fun start(block: () -> Unit) { start = block }
        fun update(block: () -> Unit) { update = block }
        fun end(block: () -> Unit) { end = block }

        fun getSpookers(): List<BotEntity> {
            val level = entity.level as? ServerLevel ?: return emptyList()
            val blockPos = entity.blockPos ?: return emptyList()
            val bounds = AABB.ofSize(blockPos.center, 20.0, 20.0, 20.0)
            return level.getEntities(EntityTypeTest.forClass(BotEntity::class.java), bounds) { entity ->
                entity.botId?.path == "singing_clergyman"
            }
        }
    }

    inner class TriggerContext() {
        fun isDark(): Boolean {
            val level = entity.level as? ServerLevel ?: return false
            return level.lightEngine.getRawBrightness(entity.blockPos, 0) < 2
        }
        fun isCaveLike(): Boolean {
            val level = entity.level as? ServerLevel ?: return false
            val blockPos = entity.blockPos ?: return false
            return blockPos.y < -20 && isDark()
        }
        fun isSpookerNear(): Boolean {
            return ctx.getSpookers().isNotEmpty()
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
        canPlay = if (level.gameTime % 20L == 0L) trigger(triggerCtx) else canPlay
        if (canPlay && ticksUntilPlay == null && !every.first.isInfinite() && !every.second.isInfinite()) {
            ticksUntilPlay = level.random.nextIntBetweenInclusive(every.first.toTicks().toInt(), every.second.toTicks().toInt()).toLong()
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
            runCatching { start() }
        }

        // Playing
        val oldTick = ticksLeft
        if (ticksLeft > 0L) {
            runCatching { update() }
            ticksLeft -= 1
        }

        // Ending
        if (oldTick > 0L && ticksLeft == 0L) {
            ticksUntilPlay = null
            runCatching { end() }
        }
    }

    fun remove() {
        if (ticksLeft > 0L && !removed) {
            ticksLeft = 0L
            runCatching { end() }
            entity.markDirtyNotifyAll()
        }
        removed = true
    }

    companion object {
        val entities = mutableMapOf<BlockEntity, MutableSet<AmbientEventPlayer>>()

        fun register() {
            AmbientEventPlayer::class.forceLoad()
            ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register { entity, level -> }
            ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
                val sounds = entities[entity] ?: return@register
                for (sound in sounds) sound.remove()
                entities -= entity
            }
        }
    }
}