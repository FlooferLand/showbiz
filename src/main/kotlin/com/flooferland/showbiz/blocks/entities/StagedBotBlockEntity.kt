package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class StagedBotBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.StagedBot.entity!!, pos, blockState), GeoBlockEntity {
    val cache = GeckoLibUtil.createInstanceCache(this)!!

    var controllerPos: BlockPos? = null
    var botId: String = findFirstBot()

    companion object {
        val MODEL_ID_MAX = 1
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        if (level?.isClientSide == false) {
            tag.putString("Bot-Id", botId)
        }

        run {  // Save controller
            val pos = controllerPos
            val data = if (pos != null) intArrayOf(pos.x, pos.y, pos.z) else intArrayOf()
            tag.putIntArray("Bound-Controller", data)
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        var botId = tag.getStringOrNull("Bot-Id")
        if (botId == null && level?.isClientSide == false) {
            botId = findFirstBot()
        }
        botId?.let { this.botId = it }

        run {  // Load controller
            val data = tag.getIntArrayOrNull("Bound-Controller")
            controllerPos = if (data != null && data.isNotEmpty() && data.size == 3) {
                BlockPos(data[0], data[1], data[2])
            } else null
        }
    }

    fun findFirstBot() = Showbiz.addons.first().bots.entries.first().key

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() =
        ClientboundBlockEntityDataPacket.create(this)!!
}