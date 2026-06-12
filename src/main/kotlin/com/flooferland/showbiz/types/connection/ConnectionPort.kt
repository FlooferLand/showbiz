package com.flooferland.showbiz.types.connection

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.*
import net.minecraft.server.level.*
import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.network.packets.ConnectionDataPacket
import com.flooferland.showbiz.types.IPacketable
import com.flooferland.showbiz.types.IUnsafeCompoundable
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull
import com.flooferland.showbiz.utils.Extensions.getLongArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getUUIDOrNull
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import com.flooferland.showbiz.utils.Extensions.removeIfPresent
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import org.jetbrains.annotations.NotNull

enum class PortDirection {
    In, Out, Both;
}

// NOTE: In order not to replace 'data', could have a separate 'receivedData' field that external ports set,
//       and the port's 'react' can manually/automatically handle merging it into 'data'. Best of both worlds.

data class ConnectionPort<T: ConnectionData<T>>(val owner: IConnectable, val id: String, val initData: T, val direction: PortDirection, val autoUseReceived: Boolean = true, val react: ConnectionPort<T>.(T) -> Unit = {}) : IUnsafeCompoundable, IPacketable {
    val name: String get() = "${owner::class.java.name}:$id(${direction.name.firstOrNull()})"
    @NotNull var data: T = initData
    @NotNull var dataReceived: T = initData

    private var listeners = hashSetOf<ConnectionOwnerId>()

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

    fun hasListeners(): Boolean = listeners.isNotEmpty()
    fun readListeners(): HashSet<ConnectionOwnerId> = listeners
    fun removeListeners(block: (ConnectionOwnerId) -> Boolean) {
        if (direction == PortDirection.In) Showbiz.log.warn("Port $name is an input port. Failed to remove listeners")
        val removedList = mutableSetOf<ConnectionOwnerId>()
        val anyRemoved = listeners.removeIf {
            if (!block(it)) return@removeIf false
            removedList.add(it)
            true
        }
        if (anyRemoved) {
            owner.connectionChanged()
            val level = owner.grabLevel() ?: return
            removedList.forEach { it.grabConnectable(level)?.connectionChanged() }
        }
    }

    /** Binds [listening] to this port on the current block */
    fun <L> bindListener(listening: L) where L: IConnectable {
        if (direction == PortDirection.In) {
            Showbiz.log.warn("Something tried to listen to port '$name', yet this port is an input port.")
            return
        }
        ConnectionOwnerId.of(listening)?.let { listeners.add(it) }
        owner.connectionChanged()
        listening.connectionChanged()
        (owner as? BlockEntity)?.markDirtyNotifyAll()
        (listening as? BlockEntity)?.markDirtyNotifyAll()
    }

    @Throws
    override fun saveOrThrow(tag: CompoundTag) {
        // Saving listeners
        if (direction != PortDirection.In) {
            val blocksPositions = listeners.mapNotNull { (it as? ConnectionOwnerId.BlockId)?.blockPos }
            tag.putLongArray("listener_blocks", blocksPositions.map { it.asLong() })

            val entityUuids = listeners.mapNotNull { (it as? ConnectionOwnerId.EntityId)?.entityUuid }
            tag.put("listener_entities", CompoundTag().also {
                it.putInt("count", entityUuids.size)
                entityUuids.forEachIndexed { i, uuid -> it.putUUID(i.toString(), uuid) }
            })

            tag.removeIfPresent("listeners")
        }
    }

    @Throws
    override fun loadOrThrow(tag: CompoundTag) {
        listeners.clear()

        // Loading listeners
        if (direction != PortDirection.In) {
            tag.getLongArrayOrNull("listeners")?.let { listeners.addAll(it.map { pos -> ConnectionOwnerId.of(BlockPos.of(pos)) }) }
            tag.getLongArrayOrNull("listener_blocks")?.let { listeners.addAll(it.map { pos -> ConnectionOwnerId.of(BlockPos.of(pos)) }) }
            tag.getCompoundOrNull("listener_entities")?.let { tag ->
                val size = tag.getInt("count")
                for (i in 0 until size) {
                    val uuid = tag.getUUIDOrNull(i.toString()) ?: continue
                    listeners.add(ConnectionOwnerId.of(uuid))
                }
            }
        }
    }

    override fun encode(buf: FriendlyByteBuf) {
        data.encode(buf)
        dataReceived.encode(buf)
    }

    override fun decode(buf: FriendlyByteBuf) {
        data.decode(buf)
        dataReceived.decode(buf)
    }

    /** Sends the current data through this port and notifies the listeners */
    fun send() {
        val level = owner.grabLevel() ?: return

        @Suppress("UNCHECKED_CAST")
        listeners.forEach { ownerId ->
            val connectable = ownerId.grabConnectable(level) ?: return@forEach
            val listener = connectable.connectionManager.inputs[id] as? ConnectionPort<T> ?: return@forEach

            listener.dataReceived = data
            if (listener.autoUseReceived) {
                listener.data = listener.dataReceived
            }

            runCatching {
                val owner = listener.owner
                if (owner.grabLevel() == null || owner.grabRemoved()) return@runCatching
                listener.react(listener, listener.dataReceived)
            }.onFailure { Showbiz.log.error("Failed to call react on listener", it) }

            if (level is ServerLevel) {
                val byteBuf = FriendlyByteBuf(Unpooled.buffer())
                data.encode(byteBuf)

                val bytes = ByteArray(byteBuf.readableBytes())
                byteBuf.readBytes(bytes)
                byteBuf.release()

                val packet = ConnectionDataPacket(ownerId, id, bytes)
                for (player in level.players()) {
                    ServerPlayNetworking.send(player, packet)
                }
            }
        }
    }

    /** Sends a piece of data through this port and notifies the listeners */
    fun send(data: T) {
        this.data = data
        send()
    }
}