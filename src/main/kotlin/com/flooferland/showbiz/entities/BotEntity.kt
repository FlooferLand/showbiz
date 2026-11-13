package com.flooferland.showbiz.entities

import com.flooferland.showbiz.Permissions
import com.flooferland.showbiz.Permissions.Companion.mayInteractWith
import com.flooferland.showbiz.entities.base.BasePropEntity
import com.flooferland.showbiz.menus.BotSelectMenu
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModLivingEntities
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.IBotAttachment
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.collidepart.CollidePartInstance
import com.flooferland.showbiz.types.collidepart.CollidePartManager
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

// TODO: Figure out if calls to connectionChanged and entity accessors are even needed
// TODO: The connection/entity syncing logic is shared between both BotEntity and FloodlightEntity, should prob unify them somehow

/**
 * The main class of the mod.
 * Has to be a LivingEntity unfortunately to cast shadows when using shaders
 */
class BotEntity(level: Level, botId: ResourceId? = null) : BasePropEntity(ModLivingEntities.Bot.type, level), GeoEntity, IConnectable, IBot, ICollidePartInteractable {
    constructor(level: Level) : this(level, null) {
        refreshDimensions()
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
    override val botOwnerId get() = OwnerId.ofEntity(this)
    override var botId: ResourceId? = null
        set(value) {
            field = value
            if (level() is ServerLevel) updatePersistentData { it.putString("bot_id", value?.toString() ?: "") }
        }
    override var botVariant: String = IBot.DEFAULT_VARIANT
        set(value) {
            field = value
            if (level() is ServerLevel) updatePersistentData { it.putString("bot_variant", value) }
        }

    override val collidePartInstance = CollidePartManager.create(this) {
        bonePrepare(this, this@BotEntity)
    }

    private var prevBotId: ResourceId? = null
    private var prevBotVariant: String = botVariant
    private val pendingShow = PackedShowData()

    init {
        this.botId = botId
        refreshDimensions()
        updatePersistentData { addAdditionalSaveData(it) }
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions = EntityDimensions.fixed(0.6f, 2.0f)

    override fun tick() {
        super.tick()
        val level = level() ?: return
        decor?.tick(this)
        soundHandler?.tick(this)
        if (!level.isClientSide) {
            show.data.tempReset()
            show.data.merge(pendingShow)
            pendingShow.tempReset()
        }

        // Updating stuff when the bot switches
        val id = OwnerId.of(this)
        if (id != null) collidePartInstance.tick(level, id)
        if (botId != prevBotId || botVariant != prevBotVariant) {
            if (id != null) collidePartInstance.refresh(level, id)
            prevBotId = botId
            prevBotVariant = botVariant
        }
    }

    override fun makeItem() = ItemStack(ModItems.Bot.item).also {
        it.set(ModComponents.BotId.type, botId)
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult? {
        if (!player.mayInteractWith(this)) return InteractionResult.FAIL

        // Grabbing the bot
        if (player.isCrouching) return grab(player)

        // Opening up the selection screen
        val id = OwnerId.of(this)
        if (id != null && Permissions.EditScreenAccess.test(player)) {
            player.openMenu(object : ExtendedScreenHandlerFactory<BotListSelectPacket> {
                override fun getDisplayName() = Component.empty()
                override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
                    val player = player as? ServerPlayer ?: return null
                    return BotSelectMenu(containerId, getScreenOpeningData(player))
                }
                override fun getScreenOpeningData(player: ServerPlayer) = BotListSelectPacket(id, botId, botVariant)
            })
        }
        return InteractionResult.SUCCESS
    }

    override fun connectionChanged() {
        if (!level().isClientSide) updatePersistentData { connectionManager.save(it) }
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        tag.putString("bot_id", botId?.toString() ?: "")
        tag.putString("bot_variant", botVariant)
        super.addAdditionalSaveData(tag)
        connectionManager.save(tag)
    }
    override fun readAdditionalSaveData(tag: CompoundTag) {
        botId = tag.getStringOrNull("bot_id")?.let { if (it.isNotBlank()) ResourceId.of(it) else null }
        tag.getStringOrNull("bot_variant")?.let { if (it.isNotBlank()) botVariant = it }
        super.readAdditionalSaveData(tag)
        connectionManager.load(tag)
    }

    companion object {
        var soundHandler: IBotAttachment? = null
        var decor: IBotAttachment? = null
        val bonePrepare: CollidePartInstance.BonePrepare.(IBot) -> Unit = bonePrepare@ {
            val botId = it.botId ?: return@bonePrepare
            when {
                botId.matches("showbiz:rolfe_dewolfe") -> {
                    map("cymbal", CollidePartId.Cymbal)
                    map("stickInner", CollidePartId.Stick)
                }
                botId.matches("showbiz-wp5:mini_mozzarella") || botId.matches("showbiz-wp5:queenie") -> {
                    map("Booper", CollidePartId.Boop)
                }
                botId.matches("showbiz:dook_larue") || botId.matches("luce_rae:dook2") -> {
                    map("LeftStickCollision", CollidePartId.LeftStick)
                    map("RightStickCollision", CollidePartId.RightStick)
                    map("HiHatCollision", CollidePartId.HiHat)
                    map("CymbalCollision", CollidePartId.Cymbal)
                    map("KickFootCollision", CollidePartId.KickFoot)
                    map("Kick", CollidePartId.Kick)
                    map("Snare", CollidePartId.Snare)
                }
            }
        }
    }
}