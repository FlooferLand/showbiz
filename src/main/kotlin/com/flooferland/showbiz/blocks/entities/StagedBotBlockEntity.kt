package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.menus.BotSelectMenu
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.IBotSoundHandler
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
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class StagedBotBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.StagedBot.entityType!!, pos, blockState), GeoBlockEntity, IConnectable, IBot, ExtendedScreenHandlerFactory<BotListSelectPacket>, ICollidePartInteractable {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In, autoUseReceived = false) { received ->
        pendingShow.merge(received)
    }

    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override var botId: ResourceId? = null

    override val collidePartInstance = CollidePartManager.create(this) {
        val botId = botId ?: return@create
        when {
            botId.matches("showbiz:rolfe_dewolfe") -> {
                map("cymbal", CollidePartId.Cymbal)
                map("stick", CollidePartId.Stick)
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

        decor?.tick(this, level, pos, state)
        soundHandler?.tick(this, level, pos, state)
        collidePartInstance.tick(level, pos, state)
        if (botId != prevBotId) {
            collidePartInstance.refresh(level, pos)
            prevBotId = botId
        }
    }

    override fun getDisplayName() = Component.literal("Staged Bot")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return BotSelectMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer): BotListSelectPacket =
        BotListSelectPacket(worldPosition, botId)

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        connectionManager.save(tag)
        // pendingShow.save(tag)

        if (level?.isClientSide == false) {
            botId?.let { tag.putString("bot_id", it.toString()) }
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
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!

    interface IDecor {
        fun tick(owner: StagedBotBlockEntity, level: Level, pos: BlockPos, state: BlockState)
    }
    companion object {
        var soundHandler: IBotSoundHandler? = null
        var decor: IDecor? = null
    }
}