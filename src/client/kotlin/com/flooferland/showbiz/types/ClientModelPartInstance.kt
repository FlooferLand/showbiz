package com.flooferland.showbiz.types

import com.flooferland.showbiz.ClientPackets
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.entities.ModelPartEntity
import com.flooferland.showbiz.network.packets.ModelPartNamesPacket
import com.flooferland.showbiz.types.modelpart.IModelPartInteractable
import com.flooferland.showbiz.types.modelpart.ModelPart
import com.flooferland.showbiz.types.modelpart.ModelPartManager
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.renderer.GeoBlockRenderer
import kotlin.jvm.optionals.getOrNull
import kotlin.math.cos
import kotlin.math.sin

class ClientModelPartInstance(val owner: IModelPartInteractable, val baseResourcePath: ResourceLocation, val customParts: Map<String, ModelPart>) : ModelPartManager.IInstance {
    val interactableParts = mutableMapOf<String, ModelPart>()
    val interactionMapping = owner.getInteractionMapping()
    var nameMapping = owner.getNameMapping()
    val trackedEntities = mutableMapOf<Int, ModelPartEntity>()

    val ownerEntity get() = owner as BlockEntity

    private val renderer get() = Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(ownerEntity) as? GeoBlockRenderer
    private val model get() = renderer?.geoModel

    private val modelResourcePath get() = model?.getModelResource(ownerEntity)?.let {
        it.withPath(it.path.replace("geo/block/", "").replace(".geo.json", ""))
    } ?: baseResourcePath

    /** Spawns all the interactable entities */
    private fun spawn() {
        val level = ownerEntity.level as? ClientLevel ?: return
        val modelPartData = ModelPartManager.modelPartData
        if (modelPartData == null) {
            Showbiz.log.error("Data returned from the model data reload listener is empty. Interaction with blocks won't be possible, consider removing any resource packs.")
            return
        }
        val modelParts = modelPartData[modelResourcePath] ?: run {
            if (customParts.isNotEmpty()) return@run hashMapOf()
            Showbiz.log.error("No model part data found for '${modelResourcePath}'")
            return
        }
        for ((key, part) in customParts) {
            modelParts[key] = part
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
        for ((partId, part) in interactableParts) {
            val pos = facing?.let {
                val angle = (it.toYRot() - 90.0) * Mth.DEG_TO_RAD
                Vec3(
                    part.pos.x * cos(angle) - part.pos.z * sin(angle),
                    part.pos.y,
                    part.pos.x * sin(angle) + part.pos.z * cos(angle)
                )
            } ?: part.pos
            val entity = ModelPartEntity(level, partId, pos, part.size, ownerEntity.blockPos)
            level.addEntity(entity)
            trackedEntities[entity.id] = entity
        }
        updateEntities()
    }

    // Killing al the entities
    override fun kill() {
        val level = ownerEntity.level as? ClientLevel ?: return
        for (id in trackedEntities.keys) {
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
            if (trackedEntities.isEmpty())
                spawn()
        } else {
            kill()
        }
    }

    private fun updateEntities() {
        for (entity in trackedEntities.values) {
            val partId = entity.partId ?: continue
            entity.displayName = (interactionMapping[partId]?.let { nameMapping[it] }) ?: partId
        }
    }

    companion object {
        init {
            ClientPackets.listen(ModelPartNamesPacket.type) { packet, context ->
                val player = context.player()
                val level = player.level() as ClientLevel
                val owner = level.getBlockEntity(packet.parent) as? IModelPartInteractable ?: return@listen
                val self = owner.modelPartInstance.clientInstance as? ClientModelPartInstance ?: return@listen
                self.nameMapping = packet.names
                self.updateEntities()
            }
        }
    }
}