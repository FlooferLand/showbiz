package com.flooferland.showbiz.types

import net.minecraft.core.*
import net.minecraft.resources.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.registry.ModBlocks
import org.joml.Vector3d

/** Responsible for initializing parts */
object ModelPartManager {
    var modelPartData: ModelPartMap? = null
    var clientModelPartInstancer: (IModelPartInteractable, block: ModBlocks, customParts: Map<String, ModelPart>) -> IInstance? = { _, _, _ -> null }

    @DslMarker
    annotation class ModelPartDsl

    @ModelPartDsl
    data class ModelPartBuilder(val parts: HashMap<String, ModelPart> = hashMapOf()) {
        public fun addPart(id: String, pos: Vec3, size: Vec3) {
            parts[id] = ModelPart(
                parent = null,
                pivot = Vec3.ZERO,
                pos = Vec3(pos.z, pos.y, pos.x).scale(8.0),
                rotation = Vector3d(0.0),
                size = size.scale(8.0)
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun create(owner: IModelPartInteractable, block: ModBlocks, builder: ModelPartBuilder.() -> Unit = {}) =
        ModelPartInstance(
            owner,
            block,
            clientModelPartInstancer(owner, block, ModelPartBuilder().also { builder(it) }.parts)
        )

    fun getMaxReach(player: Player) = if (player.isCreative) 4f else 2.7f

    interface IInstance {
        fun kill() {}
        fun tick(level: Level, pos: BlockPos, state: BlockState) {}
        fun changeResourcePath(path: ResourceLocation) {}
    }
}