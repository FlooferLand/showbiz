package com.flooferland.showbiz.types

import net.minecraft.nbt.*
import net.minecraft.server.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.properties.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.DataMigrationSteps.legacyChecks
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

typealias DataMigrationStep = (BlockEntity) -> String?
typealias LegacyCheck = (states: List<CompoundTag>, entity: CompoundTag) -> Boolean

/**
 * Don't you love that Minecraft lacks a way for mods to change things between their versions?
 * Introducing, THIS SHIT
 * All shiny and new, just instance it into your fucking block entity, and boom, well fucking done!
 * Don't forget to fucking call the fucking save and load functions as well!
 * Fucking phenomenal!
 */
class TheFuckingVersionMigrator(private val owner: BlockEntity, private val steps: List<DataMigrationStep>, private val runSteps: Boolean = true) {
    private var dataVersion = steps.size

    fun save(tag: CompoundTag) {
        //tag.remove(VERSION_KEY)
        if (dataVersion < steps.size) tag.putInt(VERSION_KEY, dataVersion)
    }
    fun load(tag: CompoundTag) {
        if (tag.contains(VERSION_KEY)) dataVersion = tag.getInt(VERSION_KEY)
    }

    fun setLevel(entity: BlockEntity) {
        if (entity.level !is ServerLevel) return
        if (dataVersion >= steps.size) return

        IVersionMigrator.queuedMigrations.add {
            val pendingSteps = steps.drop(dataVersion)
            if (!entity.isRemoved && pendingSteps.isNotEmpty()) {
                if (runSteps) pendingSteps.forEachIndexed { i, step ->
                    val err = step(entity)
                    err?.let {
                        Showbiz.log.error("Failed to migrate block at ${entity.blockPos}: $it (step $i failed)")
                        return@add
                    }
                }
                dataVersion = steps.size
                entity.setChanged()
            }
        }
    }

    companion object {
        const val VERSION_KEY = "data_version"
    }
}

interface IVersionMigrator {
    val migrator: TheFuckingVersionMigrator
    companion object {
        val queuedMigrations = ArrayDeque<() -> Unit>()

        @JvmStatic
        fun tagLegacy(chunk: CompoundTag) {
            val entities = chunk.getList("block_entities", Tag.TAG_COMPOUND.toInt())
                .filterIsInstance<CompoundTag>()
                .filter { it.getString("id") in legacyChecks && !it.contains(TheFuckingVersionMigrator.VERSION_KEY) }
            if (entities.isEmpty()) return

            val states = chunk.getList("sections", Tag.TAG_COMPOUND.toInt())
                .filterIsInstance<CompoundTag>()
                .flatMap { it.getCompound("block_states").getList("palette", Tag.TAG_COMPOUND.toInt()) }
                .filterIsInstance<CompoundTag>()

            for (entity in entities) {
                if (legacyChecks[entity.getString("id")]?.invoke(states, entity) == true) {
                    entity.putInt(TheFuckingVersionMigrator.VERSION_KEY, 0)
                }
            }
        }

        fun make(entity: BlockEntity, steps: List<DataMigrationStep>) = TheFuckingVersionMigrator(entity, steps)

        @Deprecated("Nuhuh", ReplaceWith("make"))
        fun makeTemporaryTransition(entity: BlockEntity, steps: List<DataMigrationStep>) = TheFuckingVersionMigrator(entity, steps, runSteps = false)

        fun register() {
            ServerTickEvents.END_SERVER_TICK.register {
                while (queuedMigrations.isNotEmpty()) {
                    queuedMigrations.removeFirst()()
                }
            }
        }
    }
}

object DataMigrationSteps {
    val legacyChecks = mapOf<String, LegacyCheck>(
        "showbiz:show_parser" to { states, _ ->
            states.any { it.getString("Name") == "showbiz:show_parser" && it.getCompound("Properties").contains("playing") }
        }
    )
    val showParser: List<DataMigrationStep> = listOf(
        v060@ { entity ->
            val level = entity.level ?: return@v060 "Failed to get the level"
            val facingProp = BlockStateProperties.HORIZONTAL_FACING
            val state = entity.blockState
            if (!state.hasProperty(facingProp)) return@v060 "Failed to find the facing prop"
            level.setBlock(
                entity.blockPos,
                state.setValue(facingProp, state.getValue(facingProp).opposite),
                Block.UPDATE_ALL
            )
            null
        }
    )
}
