package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.Vec3
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.menus.BotSelectMenu
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.AbstractBotPart
import com.flooferland.showbiz.types.BotPartId
import com.flooferland.showbiz.types.IBotSoundHandler
import com.flooferland.showbiz.types.ResourceId
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

class StagedBotBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.StagedBot.entityType!!, pos, blockState), GeoBlockEntity, IConnectable, ExtendedScreenHandlerFactory<BotListSelectPacket> {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In)

    val cache = GeckoLibUtil.createInstanceCache(this)!!
    var botId: ResourceId? = null
    var clientBotParts = hashMapOf<BotPartId, AbstractBotPart>()

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    private var prevBotId: ResourceId? = null

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        soundHandler?.tick(this, level, pos, state)
        if (botId != prevBotId) {
            refreshBotParts()
            prevBotId = botId
        }
    }

    fun refreshBotParts() {
        val level = level ?: return
        if (!level.isClientSide) return
        clientBotParts.values.removeIf { it.remove(Entity.RemovalReason.DISCARDED); true }
        val ids = when (botId.toString()) {
            "showbiz:rolfe_dewolfe" -> arrayOf(BotPartId.RolfeStick, BotPartId.RolfeCymbal)
            else -> emptyArray()
        }
        for (id in ids) {
            val spawn = AbstractBotPart.clientSpawn ?: continue
            val entity = spawn(level, id, this, Vec3(0.1, 0.1, 0.1))
            entity.setPos(blockPos.center)
            clientBotParts[id] = entity
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

        if (level?.isClientSide == false) {
            botId?.let { tag.putString("bot_id", it.toString()) }
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        connectionManager.load(tag)

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

    companion object {
        var soundHandler: IBotSoundHandler? = null
    }
}