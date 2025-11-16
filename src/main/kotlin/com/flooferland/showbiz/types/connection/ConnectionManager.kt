package com.flooferland.showbiz.types.connection

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull
import com.flooferland.showbiz.utils.Extensions.getLongOrNull
import com.flooferland.showbiz.utils.Extensions.getOrNull

class ConnectionManager(val owner: IConnectable, val register: ConnectionManagerRegistrar.(ConnectionManager) -> Unit) {
    val inputs = mutableMapOf<String, DataChannelIn<*>>()
    val outputs = mutableMapOf<String, DataChannelOut<*>>()

    /** This block's DataChannelOut listeners mapped to listeners' block position and channel */
    val listeners = mutableMapOf<DataChannelOut<*>, MutableList<Receiver>>()

    /** This block's functions that receive and use data */
    val receivers = mutableMapOf<DataChannelIn<*>, (Any?) -> Unit>()

    private var levelBacking: Level? = null
    val level: Level?
        get() {
            if (levelBacking == null) {
                levelBacking = when (owner) {
                    is BlockEntity -> owner.level
                    is Entity -> owner.level()
                    else -> null
                }
            }
            return levelBacking
        }

    init {
        // Registering
        ConnectionManagerRegistrar().apply { register(this@ConnectionManager) }
    }

    data class Receiver(val pos: BlockPos, val channelId: String) {
        constructor(pos: BlockPos, channel: DataChannelIn<*>) : this(pos, channel.id)
    }
    inner class ConnectionManagerRegistrar {
        public fun <T> bind(input: DataChannelIn<T>, receive: (T) -> Unit) {
            @Suppress("UNCHECKED_CAST")
            receivers[input] = receive as (Any?) -> Unit
            inputs[input.id] = input
        }
        public fun <T> bind(output: DataChannelOut<T>) {
            outputs[output.id] = output
        }
    }

    /** Broadcasts some data to all listeners of this channel */
    public fun <T> send(output: DataChannelOut<T>, data: T) {
        val receivers = listeners[output] ?: return
        val level = level ?: return

        receivers.forEach { (targetPos, inputId) ->
            val entity = level.getBlockEntity(targetPos) ?: return@forEach
            if (entity !is IConnectable) {
                Showbiz.log.warn("Block entity at '${entity.blockPos}' does not implement ${IConnectable::class.simpleName}")
                return@forEach
            }
            entity.applyChange(true) {
                entity.connectionManager.receivers.forEach { (channel, receiver) ->
                    if (channel.id == inputId) receiver.invoke(data)
                }
            }
        }
    }

    /** Binds [ours] to [theirs] */
    public fun <T> bindListener(ours: DataChannelOut<T>, theirs: Receiver): Boolean {
        val listeners = this.listeners.getOrPut(ours, { mutableListOf() })
        if (listeners.none { it.channelId == theirs.channelId }) {
            listeners.add(theirs)
            GlobalConnections.updateConnections(this, owner)
            return true
        }
        GlobalConnections.updateConnections(this, owner)
        return false
    }

    fun save(saveTag: CompoundTag) {
        val tag = CompoundTag()
        listeners.forEach { (output, receivers) ->
            val map = CompoundTag()
            receivers.forEach { (pos, channeld) ->
                map.putLong(channeld, pos.asLong())
            }
            tag.put(output.id, map)
        }
        saveTag.put("Listeners", tag)
    }
    fun load(loadTag: CompoundTag) {
        listeners.clear()

        val tag = loadTag.getOrNull("Listeners") as? CompoundTag
        tag?.allKeys?.forEach { outputId ->
            val output = outputs[outputId] ?: return@forEach
            val listenerTag = tag.getCompoundOrNull(outputId) ?: return@forEach

            listenerTag.allKeys.forEach { channelId ->
                val pos = BlockPos.of(listenerTag.getLongOrNull(channelId) ?: return@forEach)
                listeners.getOrPut(output) { mutableListOf() }
                    .add(Receiver(pos, channelId))
            }
        }

        GlobalConnections.updateConnections(this, owner)
    }
}