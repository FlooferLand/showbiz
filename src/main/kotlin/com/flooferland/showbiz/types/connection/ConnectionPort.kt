package com.flooferland.showbiz.types.connection

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.IUnsafeCompoundable
import com.flooferland.showbiz.utils.Extensions.getLongArrayOrNull
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import org.jetbrains.annotations.NotNull

enum class PortDirection {
    In, Out, Both;
}

data class ConnectionPort<T: ConnectionData>(val owner: IConnectable, val id: String, val initData: T, val direction: PortDirection, val react: ConnectionPort<T>.(T) -> Unit = {}) : IUnsafeCompoundable {
    val name: String get() = "${owner::class.java.name}:$id(${direction.name.firstOrNull()})"
    @NotNull var data: T = initData

    private var listeners = hashSetOf<BlockPos>()
    fun hasListeners(): Boolean = listeners.isNotEmpty()
    fun readListeners(): HashSet<BlockPos> = listeners
    fun removeListeners(block: (BlockPos) -> Boolean) {
        if (direction == PortDirection.In) Showbiz.log.warn("Port $name is an input port. Failed to remove listeners")
        listeners.removeIf { block(it) }
    }

    init {
        when (direction) {
            PortDirection.In -> owner.connectionManager.addInput(this)
            PortDirection.Out -> owner.connectionManager.addOutput(this)
            PortDirection.Both -> {
                owner.connectionManager.addInput(this)
                owner.connectionManager.addOutput(this)
            }
        }
    }

    @Throws
    override fun saveOrThrow(tag: CompoundTag) {
        tag.put("data", CompoundTag().also { data.saveOrThrow(it) })

        // Saving listeners
        if (direction != PortDirection.In) {
            tag.putLongArray("listeners", listeners.map { it.asLong() })
        }
    }

    @Throws
    override fun loadOrThrow(tag: CompoundTag) {
        data.loadOrThrow(tag.getCompound("data"))

        // Loading listeners
        if (direction != PortDirection.In) {
            tag.getLongArrayOrNull("listeners")?.let { array ->
                listeners.clear()
                listeners.addAll(array.map { BlockPos.of(it) })
            }
        }
    }

    /** Sends the current data through this port and notifies the listeners */
    fun send() {
        (owner as? BlockEntity)?.level?.let { level ->
            @Suppress("UNCHECKED_CAST")
            listeners.forEach { pos ->
                val entity = level.getBlockEntity(pos) as? IConnectable ?: return@forEach
                val listener = entity.connectionManager.inputs[id] as? ConnectionPort<T> ?: return@forEach

                listener.data = data
                runCatching { listener.react(listener, data) }.onFailure { Showbiz.log.error("Failed to call react on listener", it) }
                if (!level.isClientSide && entity is BlockEntity) {
                    entity.markDirtyNotifyAll()
                }
            }
        }
    }

    /** Sends a piece of data through this port and notifies the listeners */
    fun send(data: T) {
        this.data = data
        send()
    }

    /** Binds [listening] to this port on the current block */
    fun <L> bindListener(listening: L) where L: IConnectable, L: BlockEntity {
        if (direction == PortDirection.In) {
            Showbiz.log.warn("Something tried to listen to port '$name', yet this port is an input port.")
            return
        }
        listeners.add(listening.blockPos)
    }
}