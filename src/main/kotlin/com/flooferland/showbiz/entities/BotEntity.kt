package com.flooferland.showbiz.entities

import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.network.syncher.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.damagesource.*
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.*
import net.minecraft.world.entity.item.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.menus.BotSelectMenu
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModLivingEntities
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.collidepart.CollidePartManager
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import com.flooferland.showbiz.utils.Extensions.handItem
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

// TODO: Figure out if calls to connectionChanged and entity accessors are even needed

/**
 * The main class of the mod.
 * Has to be a LivingEntity unfortunately to cast shadows when using shaders
 */
class BotEntity(level: Level, botId: ResourceId? = null) : LivingEntity(ModLivingEntities.Bot.type, level), GeoEntity, IConnectable, IBot, ICollidePartInteractable {
    constructor(level: Level) : this(level, null) {
        // EntityDimensions.fixed(0.6f, 2.0f)
    }
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun getAnimatableInstanceCache() = cache
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) = Unit

    override val connectionManager = ConnectionManager(this)
    override val show = connectionManager.port("show", PackedShowData(), PortDirection.In, autoUseReceived = false) { received ->
        pendingShow.merge(received)
    }

    override val botLevel: Level? get() = level()
    override val botPos: Vec3 get() = position()
    override val botRemoved get() = isRemoved
    override var botId: ResourceId? = null
        set(value) {
            field = value
            if (level() is ServerLevel) updatePersistentData { it.putString("bot_id", value?.toString() ?: "") }
        }

    override val collidePartInstance = CollidePartManager.create(this) {
        val botId = this@BotEntity.botId ?: return@create
        when {
            botId.matches("showbiz:rolfe_dewolfe") -> {
                map("cymbal", CollidePartId.Cymbal)
                map("stick", CollidePartId.Stick)
            }

            botId.matches("showbiz-wp5:mini_mozzarella") -> {
                map("Booper", CollidePartId.Boop)
            }
        }
    }

    private var prevBotId: ResourceId? = null
    private var killDelayTicks = 0
    private val pendingShow = PackedShowData()

    init {
        this.botId = botId
        refreshDimensions()
    }

    override fun isPushable() = false
    override fun isPickable() = true
    override fun isAttackable() = true
    override fun fireImmune() = true
    override fun canBeCollidedWith() = true
    override fun canBeHitByProjectile() = true
    override fun canBeAffected(effect: MobEffectInstance) = false
    override fun canBeSeenAsEnemy() = false
    override fun getPickResult() = makeItem()

    override fun tick() {
        super.tick()
        val level = level() ?: return
        if (!level.isClientSide) {
            show.data.tempReset()
            show.data.merge(pendingShow)
            pendingShow.tempReset()
        }
        if (killDelayTicks > 0) {
            killDelayTicks -= 1
            if (killDelayTicks == 0) {
                drop()
                remove(RemovalReason.DISCARDED)
                return
            }
        }

        val id = OwnerId.of(this)
        if (id != null) collidePartInstance.tick(level, id)
        if (botId != prevBotId) {
            if (id != null) collidePartInstance.refresh(level, id)
            prevBotId = botId
        }
    }

    fun makeItem() = ItemStack(ModItems.Bot.item).also {
        it.set(ModComponents.BotId.type, botId)
    }

    fun drop() {
        val pos = position() ?: return
        val level = level() ?: return
        val item = ItemEntity(level, pos.x, pos.y + 0.5, pos.z, makeItem())
        level.addFreshEntity(item)
    }
    fun grab(player: Player): InteractionResult {
        player.handItem(makeItem())
        remove(RemovalReason.DISCARDED)
        return InteractionResult.SUCCESS
    }

    override fun isInvulnerableTo(source: DamageSource) = source.entity !is Player
    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (killDelayTicks > 0 || isRemoved) return false
        val attacker = source.entity
        fun playSound(sound: SoundEvent) {
            level().playSound(null, blockPosition(), sound, SoundSource.NEUTRAL, 1.0f, 1.0f)
        }
        if (attacker !is Player) return false

        val isClient = attacker.level().isClientSide
        if (!isClient && amount > 0f) {
            playSound(SoundEvents.ARMOR_STAND_BREAK)
            killDelayTicks = 2
        }
        return true
    }

    // region | LivingEntity stuff
    override fun getMainArm() = HumanoidArm.RIGHT
    override fun getArmorSlots() = listOf<ItemStack>()
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = ItemStack.EMPTY
    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {}
    override fun isNoGravity() = true
    override fun isPushedByFluid() = false
    override fun knockback(strength: Double, x: Double, z: Double) {}
    override fun aiStep() {}
    override fun getXRot() = 0f
    override fun getMaxHeadRotationRelativeToBody() = 0f
    // endregion

    override fun interact(player: Player, hand: InteractionHand): InteractionResult? {
        // Grabbing the bot
        if (player.isCrouching) return grab(player)

        // Opening up the selection screen
        val id = OwnerId.of(this)
        if (id != null) {
            player.openMenu(object : ExtendedScreenHandlerFactory<BotListSelectPacket> {
                override fun getDisplayName() = Component.empty()
                override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
                    val player = player as? ServerPlayer ?: return null
                    return BotSelectMenu(containerId, getScreenOpeningData(player))
                }
                override fun getScreenOpeningData(player: ServerPlayer) = BotListSelectPacket(id, botId)
            })
        }
        return InteractionResult.SUCCESS
    }

    fun updatePersistentData(block: (CompoundTag) -> Unit) {
        val tag = entityData.get(persistentDataAccessor).copy()
        block(tag)
        entityData.set(persistentDataAccessor, tag)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(persistentDataAccessor, CompoundTag().also { it.putString("bot_id", botId?.toString() ?: "") })
    }

    override fun onSyncedDataUpdated(dataAccessor: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(dataAccessor)
        if (dataAccessor == persistentDataAccessor && level().isClientSide) {
            val tag = entityData.get(persistentDataAccessor)
            readAdditionalSaveData(tag)
        }
    }

    override fun connectionChanged() {
        if (!level().isClientSide) updatePersistentData { connectionManager.save(it) }
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        tag.putString("bot_id", botId?.toString() ?: "")
        connectionManager.save(tag)
    }
    override fun readAdditionalSaveData(tag: CompoundTag) {
        botId = tag.getStringOrNull("bot_id")?.let { if (it.isNotBlank()) ResourceId.of(it) else null }
        if (!level().isClientSide) entityData.set(persistentDataAccessor, tag)
        connectionManager.load(tag)
    }

    companion object {
        val persistentDataAccessor = SynchedEntityData.defineId(BotEntity::class.java, EntityDataSerializers.COMPOUND_TAG)!!
    }
}