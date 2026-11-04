package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.menus.BotSelectMenu
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.IBotAttachment
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
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class StagedBotBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.StagedBot.entityType!!, pos, blockState), GeoBlockEntity, IConnectable, IBot, ExtendedScreenHandlerFactory<BotListSelectPacket>, ICollidePartInteractable {
    override val connectionManager = ConnectionManager(this)
    override val show = connectionManager.port("show", PackedShowData(), PortDirection.In, autoUseReceived = false) { received ->
        pendingShow.merge(received)
    }

    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override var botId: ResourceId? = null
    override var botVariant: String = IBot.DEFAULT_VARIANT
    override val botOwnerId get() = OwnerId.ofEntity(this)
    override val botLevel get() = level
    override val botPos get() = blockPos.above().bottomCenter!!

    override val collidePartInstance = CollidePartManager.create(this) {
        val botId = botId ?: return@create
        when {
            botId.matches("showbiz:rolfe_dewolfe") -> {
                map("cymbal", CollidePartId.Cymbal)
                map("stickInner", CollidePartId.Stick)
            }
            botId.matches("showbiz-wp5:mini_mozzarella") -> {
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

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    private val pendingShow = PackedShowData();
    private var prevBotId: ResourceId? = null

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (!level.isClientSide) {
            show.data.tempReset()
            show.data.merge(pendingShow)
            pendingShow.tempReset()
        }

        decor?.tick(this)
        soundHandler?.tick(this)

        val id = OwnerId.of(this)
        if (id != null) collidePartInstance.tick(level, id)
        if (botId != prevBotId) {
            if (id != null) collidePartInstance.refresh(level, id)
            prevBotId = botId
        }
    }

    override fun getDisplayName() = Component.literal("Staged Bot")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return getScreenOpeningData(player)?.let { BotSelectMenu(i, it) }
    }
    override fun getScreenOpeningData(player: ServerPlayer): BotListSelectPacket? =
        OwnerId.of(this)?.let { BotListSelectPacket(it, botId, botVariant) }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        connectionManager.save(tag)
        // pendingShow.save(tag)

        if (level?.isClientSide == false) {
            botId?.let { tag.putString("bot_id", it.toString()) }
            botVariant.let { tag.putString("bot_variant", it) }
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        connectionManager.load(tag)
        // pendingShow.load(tag)

        tag.getStringOrNull("bot_id")?.let { botId ->
            var id = ResourceId.of(botId)
            if (id == null) {  // Converting old bot ids (no namespace)
                id = Showbiz.bots.keys.firstOrNull() { it.path == botId }
                Showbiz.log.debug("Converted old bot '{}' to the new resource id '{}'", botId, id)
            }
            this.botId = id
        }
        tag.getStringOrNull("bot_variant")?.let { botVariant ->
            this.botVariant = botVariant
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!

    companion object {
        var soundHandler: IBotAttachment? = null
        var decor: IBotAttachment? = null
    }
}