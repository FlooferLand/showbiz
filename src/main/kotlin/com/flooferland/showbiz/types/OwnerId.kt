package com.flooferland.showbiz.types

import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.utils.Extensions.getEntity
import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import java.util.UUID

/** General class that can store either a block position or an entity UUID */
sealed class OwnerId() {
    abstract fun grabConnectable(level: Level): IConnectable?
    abstract fun grabBlockPos(level: Level): BlockPos?
    abstract fun grabPos(level: Level): Vec3?
    abstract fun isLoaded(level: Level): Boolean
    abstract fun isRemoved(level: Level): Boolean

    // TODO: Bot entities when being loaded in for some reason lose connection
    abstract fun isRemovedEntityWorkaround(level: Level): Boolean

    data class BlockId(val blockPos: BlockPos) : OwnerId() {
        override fun grabConnectable(level: Level) = level.getBlockEntity(blockPos) as? IConnectable
        override fun grabBlockPos(level: Level) = blockPos
        override fun grabPos(level: Level) = blockPos.center!!
        override fun isLoaded(level: Level): Boolean = grabBlockPos(level).let { level.isLoaded(it) }
        override fun isRemoved(level: Level): Boolean = level.getBlockEntity(blockPos)?.isRemoved ?: true
        override fun isRemovedEntityWorkaround(level: Level) = isRemoved(level)
        fun grabBlockState(level: Level): BlockState = level.getBlockState(blockPos)
        fun grabBlockEntity(level: Level) = level.getBlockEntity(blockPos)
    }

    class EntityId(val entityUuid: UUID, var entityLocalId: Int? = null) : OwnerId() {
        override fun grabConnectable(level: Level) = grabEntity(level) as? IConnectable
        override fun grabBlockPos(level: Level) = grabEntity(level)?.blockPosition()
        override fun grabPos(level: Level) = grabEntity(level)?.position()
        override fun isLoaded(level: Level) = true  // TODO: Should figure out if the entity is loaded or not
        override fun isRemoved(level: Level): Boolean = grabEntity(level)?.isRemoved ?: true
        override fun isRemovedEntityWorkaround(level: Level) = grabEntity(level)?.isRemoved ?: false
        fun grabEntity(level: Level): Entity? {
            val id = entityLocalId
            val localEntity = if (id != null && level.isClientSide) level.getEntity(id) else null
            return localEntity ?: level.getEntity(entityUuid)?.also { entityLocalId = it.id }
        }

        override fun equals(other: Any?) = other is EntityId && entityUuid == other.entityUuid
        override fun hashCode() = entityUuid.hashCode()
    }

    fun matches(connectable: IConnectable) = of(connectable)?.let { it == this } ?: false

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
        fun decode(buf: FriendlyByteBuf): OwnerId =
            if (buf.readBoolean()) {
                BlockId(buf.readBlockPos())
            } else {
                val uuid = buf.readUUID()
                val id = if (buf.readBoolean()) buf.readInt() else null
                EntityId(uuid, id)
            }

        fun of(connectable: IConnectable) = when (connectable) {
            is BlockEntity -> OwnerId.of(connectable.blockPos)
            is Entity -> OwnerId.of(connectable.uuid, connectable.id)
            else -> null
        }
        fun ofEntity(entity: Entity) = of(entity.uuid, if (entity.level().isClientSide) entity.id else null)
        fun ofEntity(entity: BlockEntity) = of(entity.blockPos)
        fun of(pos: BlockPos) = BlockId(pos)
        fun of(uuid: UUID, id: Int? = null) = EntityId(uuid, id)

        val CODEC: Codec<OwnerId> = Codec.either(
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

        val STREAM_CODEC = StreamCodec.of<FriendlyByteBuf, OwnerId>(
            { buf, id -> id.encode(buf) },
            { buf -> decode(buf) },
        )!!
    }
}