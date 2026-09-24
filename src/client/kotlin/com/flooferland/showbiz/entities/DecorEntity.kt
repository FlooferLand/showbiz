package com.flooferland.showbiz.entities

import net.minecraft.client.multiplayer.*
import net.minecraft.nbt.*
import net.minecraft.network.syncher.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.registry.ModClientEntities
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.IBotAttachment
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.types.math.Vec2f
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class DecorEntity(level: Level, val boneName: String? = null, val decorId: Id = Id.PomPom, val owner: IBot? = null) : Entity(ModClientEntities.Decor.type, level), GeoEntity {
    override fun isInvulnerable() = true
    override fun shouldBeSaved() = false
    override fun shouldRender(x: Double, y: Double, z: Double) = true
    override fun defineSynchedData(builder: SynchedEntityData.Builder) = Unit
    override fun readAdditionalSaveData(compound: CompoundTag) = Unit
    override fun addAdditionalSaveData(compound: CompoundTag) = Unit

    val size = Vec2f(0.4f, 0.8f)

    override fun isPushable() = false
    override fun isPickable() = false
    override fun isAttackable() = false
    override fun canBeCollidedWith() = false
    override fun canBeHitByProjectile() = false
    override fun getDimensions(pose: Pose): EntityDimensions = EntityDimensions.fixed(size.x, size.y)
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) = Unit
    override fun getAnimatableInstanceCache() = cache

    private var botId: ResourceId? = null

    enum class Id {
        PomPom
    }

    val cache = GeckoLibUtil.createInstanceCache(this)!!
    var oldPos = Vec3.ZERO!!
    var newPos = Vec3.ZERO!!

    init {
        id = nextEntityId.getAndDecrement()
        botId = owner?.botId
        refreshDimensions()
    }

    fun moveDecor(bonePos: Vec3) {
        val bonePos = bonePos.add(0.0, (-size.y).toDouble(), 0.0)
        newPos = bonePos
    }

    override fun tick() {
        super.tick()
        val level = level() ?: return
        val entities = decorEntities[owner]
        if (owner?.botOwnerId?.isRemoved(level) != false || entities?.contains(this) != true || botId != owner.botId) {
            entities?.remove(this)
            remove(RemovalReason.DISCARDED)
            return
        }
    }

    companion object {
        val decorEntities = WeakHashMap<IBot, MutableSet<DecorEntity>>()
        val decorTick = object : IBotAttachment {
            override fun tick(bot: IBot) {
                val level = bot.botLevel as? ClientLevel ?: return
                val botId = bot.botId ?: return
                val entities = decorEntities[bot] ?: mutableSetOf()

                if (botId.matches("showbiz:mitzi_mozzarella") && entities.count { it.decorId == Id.PomPom && it.boneName!!.startsWith("Pom") } != 2) {
                    spawn(bot, "PomL", Id.PomPom)
                    spawn(bot, "PomR", Id.PomPom)
                }
            }
        }

        /**
         * Workaround required for shaders to work.
         * Called at the end of world rendering
         */
        fun moveAll(ctx: WorldRenderContext) {
            decorEntities.forEach { (botEntity, entities) ->
                entities.forEach { it.moveTo(it.newPos) }
            }
        }

        fun spawn(owner: IBot, attachedTo: String, decorId: Id) {
            val level = owner.botLevel as? ClientLevel ?: return
            val pos = owner.botPos ?: return
            val entity = DecorEntity(level, attachedTo, decorId, owner)
            entity.moveTo(pos)
            level.addEntity(entity)

            val entities = decorEntities[owner] ?: mutableSetOf()
            entities.add(entity)
            decorEntities[owner] = entities
        }

        /// Required to manually set the ID because the client assign its ID to the player and causes the player to freeze
        private val nextEntityId = AtomicInteger(-2000)
    }
}