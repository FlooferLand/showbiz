package com.flooferland.showbiz.types.connection

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull
import com.flooferland.showbiz.utils.Extensions.getLongArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getOrNull
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*

class ConnectionManager(val owner: IConnectable, val register: ConnectionManagerRegistrar.(ConnectionManager) -> Unit) {
    private val receivers = mutableMapOf<DataChannelIn<*>, (Any?) -> Unit>()
    private val outgoing = mutableMapOf<DataChannelOut<*>, MutableSet<BlockPos>>()

    val listeners = mutableMapOf<DataChannelOut<*>, MutableList<Receiver<*>>>()

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

    data class Receiver<T>(val pos: BlockPos, val channel: DataChannelIn<T>)
    inner class ConnectionManagerRegistrar {
        public fun <T> bind(input: DataChannelIn<T>, receive: (T) -> Unit) {
            @Suppress("UNCHECKED_CAST")
            receivers[input] = receive as (Any?) -> Unit
        }
        public fun <T> bind(output: DataChannelOut<T>) = outgoing.putIfAbsent(output, mutableSetOf())
    }

    /** Broadcasts some data to all listeners of this channel */
    public fun <T> send(output: DataChannelOut<T>, data: T) {
        val receivers = listeners[output] ?: return
        val level = level ?: return

        receivers.forEach { (targetPos, input) ->
            val entity = level.getBlockEntity(targetPos) ?: return@forEach
            if (entity !is IConnectable) {
                Showbiz.log.warn("Block entity at '${entity.blockPos}' does not implement ${IConnectable::class.simpleName}")
                return@forEach
            }
            entity.applyChange(true) {
                entity.connectionManager.receivers.forEach { (channel, receiver) ->
                    if (channel.id == input.id) {
                        receiver.invoke(data)
                    }
                }
            }
        }
    }

    /** Binds [ours] to [theirs] */
    public fun <T> bindListener(ours: DataChannelOut<T>, theirs: Receiver<T>) {
        val listeners = this.listeners.getOrPut(ours, { mutableListOf() })
        Showbiz.log.debug("Connected '{}:{}' to {}", theirs.pos, theirs.channel.id, ours.id)
        listeners.add(theirs)
    }

    fun save(tag: CompoundTag) {
        val tag = CompoundTag()

        tag.put("Outgoing", CompoundTag().also { tag ->
            outgoing.forEach { (channel, positions) ->
                tag.putLongArray(channel.id, positions.map { it.asLong() })
            }
        })

        tag.put("Listeners", CompoundTag().also { tag ->
            listeners.forEach { (output, receivers) ->
                receivers.forEach { (pos, input) -> tag.putLong(input.id, pos.asLong()) }
            }
        })

        tag.put("Connections", tag)
    }
    fun load(tag: CompoundTag) {
        outgoing.clear()
        listeners.clear()

        val tag = tag.getOrNull("Connections") as? CompoundTag ?: return
        val outgoingTag = tag.getCompoundOrNull("Outgoing") ?: return
        val listenersTag = tag.getCompoundOrNull("Listeners") ?: return

        for (key in outgoingTag.allKeys) {
            val positions = tag.getLongArrayOrNull(key) ?: continue
            positions.forEach { long ->
                val found = outgoing.keys.find { it.id == key } ?: return@forEach
                val output = outgoing.getOrPut(found, { mutableSetOf() })
                val blockPos = BlockPos.of(long)
                output.add(blockPos)
            }
        }
    }
}