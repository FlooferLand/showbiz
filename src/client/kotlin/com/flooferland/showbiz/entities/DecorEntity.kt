package com.flooferland.showbiz.entities

import net.minecraft.client.multiplayer.*
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.syncher.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModClientEntities
import com.flooferland.showbiz.types.ResourceId
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class DecorEntity(level: Level, val boneName: String? = null, val decorId: Id = Id.PomPom, val owner: StagedBotBlockEntity? = null) : Entity(ModClientEntities.Decor.type, level), GeoEntity {
    override fun isInvulnerable() = true
    override fun shouldBeSaved() = false
    override fun shouldRender(x: Double, y: Double, z: Double) = true
    override fun defineSynchedData(builder: SynchedEntityData.Builder) = Unit
    override fun readAdditionalSaveData(compound: CompoundTag) = Unit
    override fun addAdditionalSaveData(compound: CompoundTag) = Unit

    override fun isPushable() = false
    override fun isPickable() = false
    override fun isAttackable() = false
    override fun canBeCollidedWith() = false
    override fun canBeHitByProjectile() = false
    override fun getDimensions(pose: Pose): EntityDimensions = EntityDimensions.fixed(0.25f, 0.25f)
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) = Unit
    override fun getAnimatableInstanceCache() = cache

    private var botId: ResourceId? = null

    enum class Id {
        PomPom
    }

    val cache = GeckoLibUtil.createInstanceCache(this)!!
    var oldPos = Vec3.ZERO!!

    init {
        id = nextEntityId.getAndDecrement()
        botId = owner?.botId
        refreshDimensions()
    }

    fun moveDecor(bonePos: Vec3) {
        setPosRaw(bonePos.x, bonePos.y, bonePos.z)
    }

    override fun tick() {
        super.tick()
        val level = level() ?: return
        val entities = decorEntities[owner]
        if (owner?.isRemoved != false || entities?.contains(this) != true || botId != owner.botId) {
            entities?.remove(this)
            remove(RemovalReason.DISCARDED)
            return
        }
    }

    companion object {
        val decorEntities = WeakHashMap<StagedBotBlockEntity, MutableSet<DecorEntity>>()
        val decorTick = object : StagedBotBlockEntity.IDecor {
            override fun tick(owner: StagedBotBlockEntity, level: Level, pos: BlockPos, state: BlockState) {
                val level = level as? ClientLevel ?: return
                val botId = owner.botId ?: return
                val entities = decorEntities[owner] ?: mutableSetOf()

                if (botId.matches("showbiz:mitzi_mozzarella") && entities.count { it.decorId == Id.PomPom && it.boneName!!.startsWith("Pom") } != 2) {
                    spawn(owner, level, "PomL", Id.PomPom)
                    spawn(owner, level, "PomR", Id.PomPom)
                }
            }
        }

        fun spawn(owner: StagedBotBlockEntity, level: ClientLevel, attachedTo: String, decorId: Id) {
            val entity = DecorEntity(level, attachedTo, decorId, owner)
            entity.setPos(owner.blockPos.above().center)
            level.addEntity(entity)

            val entities = decorEntities[owner] ?: mutableSetOf()
            entities.add(entity)
            decorEntities[owner] = entities
        }

        /// Required to manually set the ID because the client assign its ID to the player and causes the player to freeze
        private val nextEntityId = AtomicInteger(-2000)
    }
}