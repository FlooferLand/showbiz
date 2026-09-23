package com.flooferland.showbiz.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.syncher.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.damagesource.*
import net.minecraft.world.effect.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.item.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.ServerPackets
import com.flooferland.showbiz.components.FloodlightComponent
import com.flooferland.showbiz.menus.FloodlightEditMenu
import com.flooferland.showbiz.network.packets.FloodlightEditPacket
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModLivingEntities
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull
import com.flooferland.showbiz.utils.Extensions.getDoubleOrNull
import com.flooferland.showbiz.utils.Extensions.getFloatOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import com.flooferland.showbiz.utils.Extensions.getLongOrNull
import com.flooferland.showbiz.utils.Extensions.handItem
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

// TODO: The connection/entity syncing logic is shared between both BotEntity and FloodlightEntity, should prob unify them somehow

class FloodlightEntity(level: Level, comp: FloodlightComponent) : LivingEntity(ModLivingEntities.Floodlight.type, level), GeoEntity, IConnectable, EditScreenOwner<FloodlightEditPacket> {
    constructor(level: Level) : this(level, FloodlightComponent()) {}
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun getAnimatableInstanceCache() = cache
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) = Unit

    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In, autoUseReceived = false) { show ->
        lit = menuData.bitFilter.chartHasBit(show.mapping) { show.signal.frameHas(it) }
        if (!level().isClientSide) updatePersistentData { it.putBoolean("lit", lit) }
    }

    val isLit: Boolean get() = lit || redstoneSignal > 0
    var value: Float = 0f  // Used for smoothing on the client
    var redstoneSignal: Int = 0
    var supportAbove = false
    var supportBelow = false

    // Useful for keeping the same position even if the entity dimensions/model are adjusted later in mod dev
    // This helds exactly where the user originally placed the light via its item
    var supportBlock: BlockPos? = null
    var supportBlockLoc: Vec3? = null

    override var menuData = EditScreenMenu.EditScreenBuf(OwnerId.ofEntity(this), comp.bitFilter)
    var angle = comp.angle
    var shadows: Boolean = comp.shadows
    var color: Int = comp.color
    var brightness: Float = 1.0f
    var turn = comp.turn

    private var lit: Boolean = false
    var startPos = Vec3.ZERO!!
    var endPos = Vec3.ZERO!!

    init {
        updateYaw(turn.x)
        refreshDimensions()
        updatePersistentData { addAdditionalSaveData(it) }
    }
    override fun tick() {
        super.tick()
        supportAbove = Block.canSupportCenter(level(), blockPosition().above(), Direction.DOWN)
        supportBelow = Block.canSupportCenter(level(), blockPosition().below(), Direction.UP)
        if (yRot != turn.x) updateYaw(turn.x)
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
    override fun getDefaultDimensions(pose: Pose): EntityDimensions =
        EntityDimensions.fixed(0.4f, 0.9f)

    fun makeItem() = ItemStack(ModItems.Floodlight.item).also {
        it.set(ModComponents.Floodlight.type, FloodlightComponent.from(this))
    }

    fun drop() {
        val pos = position() ?: return
        val level = level() ?: return
        val item = ItemEntity(level, pos.x, pos.y + 0.5, pos.z, makeItem())
        level.addFreshEntity(item)
        remove(RemovalReason.DISCARDED)
    }
    fun grab(player: Player): InteractionResult {
        player.handItem(makeItem())
        remove(RemovalReason.DISCARDED)
        return InteractionResult.SUCCESS
    }

    override fun isInvulnerableTo(source: DamageSource) = source.entity !is Player
    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (isRemoved || isInvulnerableTo(source)) return false
        val level = level() as? ServerLevel ?: return false
        val player = source.entity as? Player ?: return false
        if (!player.mayBuild()) return false

        if (!source.isCreativePlayer)
            Block.popResource(level, blockPosition(), makeItem())
        level.playSound(null, x, y, z, SoundEvents.ARMOR_STAND_BREAK, soundSource, 1.0f, 1.0f)
        remove(RemovalReason.KILLED)
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
    override fun getXRot() = 0f
    override fun getMaxHeadRotationRelativeToBody() = 0f
    // endregion

    override fun interact(player: Player, hand: InteractionHand): InteractionResult? {
        // Grabbing the floodlight
        if (player.isCrouching) return grab(player)

        // Opening up the edit screen
        val id = OwnerId.of(this)
        if (id != null) player.openMenu(this)
        return InteractionResult.SUCCESS
    }

    override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return FloodlightEditMenu(containerId, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        FloodlightEditPacket(EditScreenMenu.EditScreenBuf(OwnerId.ofEntity(this), menuData.bitFilter, show.data.mapping), turn, angle, shadows, color, brightness)

    fun applyPacket(packet: FloodlightEditPacket) {
        menuData = packet.base
        turn = packet.turn
        angle = packet.angle
        color = packet.color
        brightness = packet.brightness
        shadows = packet.shadows
        updateYaw(turn.x)
        updatePersistentData { addAdditionalSaveData(it) }
    }

    fun updatePersistentData(block: (CompoundTag) -> Unit) {
        val tag = entityData.get(persistentDataAccessor).copy()
        block(tag)
        entityData.set(persistentDataAccessor, tag)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(persistentDataAccessor, CompoundTag())
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
        menuData.saveAdditional(tag)
        tag.putBoolean("lit", lit)
        tag.putBoolean("shadows", shadows)
        tag.putFloat("turn_x", turn.x)
        tag.putFloat("turn_y", turn.y)
        tag.putFloat("angle", angle)
        tag.putFloat("brightness", brightness)
        tag.putInt("color", color)

        supportBlock?.let { tag.putLong("support_block", it.asLong()) }
        supportBlockLoc?.let {
            tag.put("support_location", CompoundTag().also { tag ->
                tag.putDouble("x", it.x)
                tag.putDouble("y", it.y)
                tag.putDouble("z", it.z)
            })
        }

        // Stuff that can be recalculated if its lost
        tag.putInt("redstone_signal", redstoneSignal)
        tag.putBoolean("support_above", supportAbove)
        tag.putBoolean("support_below", supportBelow)

        connectionManager.save(tag)
    }
    override fun readAdditionalSaveData(tag: CompoundTag) {
        menuData.loadAdditional(tag)
        tag.getBooleanOrNull("lit")?.let { lit = it }
        tag.getBooleanOrNull("shadows")?.let { shadows = it }
        tag.getFloatOrNull("turn_x")?.let { turn.x = it }
        tag.getFloatOrNull("turn_y")?.let { turn.y = it }
        tag.getFloatOrNull("angle")?.let { angle = it }
        tag.getFloatOrNull("brightness")?.let { brightness = it }
        tag.getIntOrNull("color")?.let { color = it }

        tag.getLongOrNull("support_block")?.let { supportBlock = BlockPos.of(it) }
        tag.getCompoundOrNull("support_location")?.let { tag ->
            val x = tag.getDoubleOrNull("x")
            val y = tag.getDoubleOrNull("y")
            val z = tag.getDoubleOrNull("z")
            if (x != null && y != null && z != null) {
                supportBlockLoc = Vec3(x, y, z)
            }
        }

        // Stuff that can be recalculated if its lost
        tag.getIntOrNull("redstone_signal")?.let { redstoneSignal = it }
        tag.getBooleanOrNull("support_above")?.let { supportAbove = it }
        tag.getBooleanOrNull("support_below")?.let { supportBelow = it }

        if (!level().isClientSide) entityData.set(persistentDataAccessor, tag)
        connectionManager.load(tag)
    }

    fun updateYaw(yaw: Float) {
        yRot = turn.x
        yRotO = turn.x
        yHeadRot = turn.x
        yHeadRotO = turn.x
        yBodyRot = turn.x
        yBodyRotO = turn.x
    }

    companion object {
        val persistentDataAccessor = SynchedEntityData.defineId(FloodlightEntity::class.java, EntityDataSerializers.COMPOUND_TAG)!!
        init {
            ServerPackets.listen(FloodlightEditPacket.type) { packet, context ->
                val player = context.player() ?: return@listen
                val entity = (packet.base.id as? OwnerId.EntityId)?.grabEntity(player.serverLevel()) as? FloodlightEntity ?: return@listen
                entity.applyPacket(packet)
            }
        }
    }
}