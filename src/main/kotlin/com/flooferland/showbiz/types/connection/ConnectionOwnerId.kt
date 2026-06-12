package com.flooferland.showbiz.types.connection

import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.utils.Extensions.getEntity
import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import java.util.UUID

sealed class ConnectionOwnerId() {
    abstract fun grabConnectable(level: Level): IConnectable?
    abstract fun grabBlockPos(level: Level): BlockPos?
    abstract fun grabPos(level: Level): Vec3?

    data class BlockId(val blockPos: BlockPos) : ConnectionOwnerId() {
        override fun grabConnectable(level: Level) = level.getBlockEntity(blockPos) as? IConnectable
        override fun grabBlockPos(level: Level) = blockPos
        override fun grabPos(level: Level) = blockPos.center!!
    }

    class EntityId(val entityUuid: UUID, var entityLocalId: Int? = null) : ConnectionOwnerId() {
        override fun grabConnectable(level: Level) = grabEntity(level) as? IConnectable
        override fun grabBlockPos(level: Level) = grabEntity(level)?.blockPosition()
        override fun grabPos(level: Level) = grabEntity(level)?.position()
        fun grabEntity(level: Level): Entity? {
            val id = entityLocalId
            if (id != null && level.isClientSide) {
                return level.getEntity(id).let { if (it is IConnectable) it else null }
            }
            return level.getEntity(entityUuid)?.let {
                entityLocalId = it.id
                it as? IConnectable as Entity?
            }
        }

        override fun equals(other: Any?) = other is EntityId && entityUuid == other.entityUuid
        override fun hashCode() = entityUuid.hashCode()
    }

    fun isLoaded(level: Level): Boolean =
        grabBlockPos(level)?.let { level.isLoaded(it) } ?: false

    fun matches(connectable: IConnectable) = ConnectionOwnerId.of(connectable)?.let { it == this } ?: false

    fun encode(buf: FriendlyByteBuf) {
        when (this) {
            is BlockId -> {
                buf.writeBoolean(true)
                buf.writeBlockPos(blockPos)
            }
            is EntityId -> {
                buf.writeBoolean(false)
                buf.writeUUID(entityUuid)
                buf.writeBoolean(entityLocalId != null)
                entityLocalId?.let { buf.writeInt(it) }
            }
        }
    }

    companion object {
        fun decode(buf: FriendlyByteBuf): ConnectionOwnerId =
            if (buf.readBoolean()) {
                BlockId(buf.readBlockPos())
            } else {
                val uuid = buf.readUUID()
                val id = if (buf.readBoolean()) buf.readInt() else null
                EntityId(uuid, id)
            }

        fun of(connectable: IConnectable) = when (connectable) {
            is BlockEntity -> ConnectionOwnerId.of(connectable.blockPos)
            is Entity -> ConnectionOwnerId.of(connectable.uuid, connectable.id)
            else -> null
        }
        fun of(pos: BlockPos) = ConnectionOwnerId.BlockId(pos)
        fun of(uuid: UUID, id: Int? = null) = ConnectionOwnerId.EntityId(uuid, id)

        val CODEC: Codec<ConnectionOwnerId> = Codec.either(
            BlockPos.CODEC.fieldOf("block").codec(),
            UUIDUtil.CODEC.fieldOf("entity").codec()
        ).xmap({ either ->
            either.map(
                { BlockId(it) },
                { EntityId(it) }
            )
        }, { id ->
            when (id) {
                is BlockId -> Either.left(id.blockPos)
                is EntityId -> Either.right(id.entityUuid)
            }
        })

        val STREAM_CODEC = StreamCodec.of<FriendlyByteBuf, ConnectionOwnerId>(
            { buf, id -> id.encode(buf) },
            { buf -> decode(buf) },
        )!!
    }
}