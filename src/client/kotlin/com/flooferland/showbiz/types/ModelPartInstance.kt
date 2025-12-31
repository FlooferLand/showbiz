package com.flooferland.showbiz.types

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.entities.ModelPartEntity
import com.flooferland.showbiz.network.packets.ModelPartInteractPacket
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import software.bernie.geckolib.model.GeoModel
import kotlin.collections.iterator
import kotlin.jvm.optionals.getOrNull
import kotlin.math.cos
import kotlin.math.sin

class ModelPartInstance(val owner: IModelPartInteractable, modelResourcePath: ResourceLocation) : ModelPartManager.IInstance {
    val interactableParts = mutableMapOf<String, ModelPart>()
    val interactionMapping = owner.getInteractionMapping()
    val trackedEntities = mutableListOf<Int>()

    val ownerEntity get() = owner as BlockEntity

    private val modelResourcePath = when (owner) {
        is GeoModel<*> -> modelResourcePath.withPrefix("geckolib/models/block/").withSuffix(".geo.json")
        else -> modelResourcePath
    }

    /** Spawns all the interactable entities */
    private fun spawn() {
        val level = ownerEntity.level as? ClientLevel ?: return
        val modelPartData = ModelPartManager.modelPartData
        if (modelPartData == null) {
            Showbiz.log.error("Data returned from the model data reload listener is empty. Interaction with blocks won't be possible, consider removing any resource packs.")
            return
        }
        val modelParts = modelPartData[modelResourcePath] ?: run {
            Showbiz.log.error("No model part data found for '${modelResourcePath}'")
            return
        }

        for ((name, data) in modelParts) {
            if (interactionMapping.containsKey(name)) {
                interactableParts[name] = data
            }
        }

        // Killing the prvious entities
        kill()

        // Spawning the entities
        val facing = ownerEntity.blockState.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).getOrNull()
        for ((name, part) in interactableParts) {
            val pos = facing?.let {
                val angle = (it.toYRot() - 90.0) * Mth.DEG_TO_RAD
                Vec3(
                    part.pos.x * cos(angle) - part.pos.z * sin(angle),
                    part.pos.y,
                    part.pos.x * sin(angle) + part.pos.z * cos(angle)
                )
            } ?: part.pos
            val entity = ModelPartEntity(level, name, pos, part.size, ownerEntity.blockPos)
            level.addEntity(entity)
            trackedEntities.add(entity.id)
        }
    }

    // Killing al the entities
    override fun kill() {
        val level = ownerEntity.level as? ClientLevel ?: return
        for (id in trackedEntities) {
            level.removeEntity(id, Entity.RemovalReason.DISCARDED)
        }
        trackedEntities.clear()
    }

    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        val level = level as? ClientLevel ?: return
        val player = Minecraft.getInstance()?.player ?: return

        // Throttle level access
        if (level.gameTime % 5 != 0L) return

        val distance = player.distanceToSqr(pos.center)
        val reach = ModelPartManager.getMaxReach(player)
        if (distance <= reach * reach) {
            spawn()
        } else {
            kill()
        }
    }

    companion object {
        init {
            ServerPlayNetworking.registerGlobalReceiver(ModelPartInteractPacket.type) { packet, context ->
                val player = context.player()
                context.server().execute {
                    if (player.uuid != packet.player) return@execute
                    val level = player.serverLevel() ?: return@execute

                    // TODO: Do a more secure distance check for model part interaction (raycast)
                    if (player.position().distanceTo(packet.parent.center) > ModelPartManager.getMaxReach(player) + 1f) {
                        Showbiz.log.info("Player ${player.name} shouldn't have been able to reach a modelpart block. Reach hacks?")
                        return@execute
                    }

                    val blockEntity = level.getBlockEntity(packet.parent) as? IModelPartInteractable ?: return@execute
                    val key = blockEntity.getInteractionMapping()[packet.name] ?: return@execute
                    blockEntity.onInteract(key, level, player)
                }
            }
        }
    }
}