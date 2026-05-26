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
import com.flooferland.showbiz.utils.Extensions.getLongArrayOrNull
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
        // Saving listeners
        if (direction != PortDirection.In) {
            tag.putLongArray("listeners", listeners.map { it.asLong() })
        }
    }

    @Throws
    override fun loadOrThrow(tag: CompoundTag) {
        listeners.clear()

        // Loading listeners
        if (direction != PortDirection.In) {
            tag.getLongArrayOrNull("listeners")?.let { array ->
                listeners.addAll(array.map { BlockPos.of(it) })
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
        (owner as? BlockEntity)?.level?.let { level ->
            @Suppress("UNCHECKED_CAST")
            listeners.forEach { pos ->
                val entity = level.getBlockEntity(pos) as? IConnectable ?: return@forEach
                val listener = entity.connectionManager.inputs[id] as? ConnectionPort<T> ?: return@forEach

                listener.dataReceived = data
                if (listener.autoUseReceived) {
                    listener.data = listener.dataReceived
                }

                runCatching {
                    val be = (listener.owner as? BlockEntity)
                    if (be?.level == null || be.isRemoved) return@runCatching
                    listener.react(listener, listener.dataReceived)
                }.onFailure { Showbiz.log.error("Failed to call react on listener", it) }

                if (!level.isClientSide) {
                    val byteBuf = FriendlyByteBuf(io.netty.buffer.Unpooled.buffer())
                    data.encode(byteBuf)

                    val bytes = ByteArray(byteBuf.readableBytes())
                    byteBuf.readBytes(bytes)
                    byteBuf.release()

                    val packet = ConnectionDataPacket(pos, id, bytes)
                    for (player in (level as ServerLevel).players()) {
                        ServerPlayNetworking.send(player, packet)
                    }
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