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

    var greyboxPos: BlockPos? = null
    var botId: String? = findFirstBot()

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        if (level?.isClientSide == false) {
            botId?.let { tag.putString("Bot-Id", it) }
        }

        greyboxPos?.let {
            tag.putIntArray("Bound-Greybox", intArrayOf(it.x, it.y, it.z))
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        var botId = tag.getStringOrNull("Bot-Id")
        if (botId == null && level?.isClientSide == false) {
            botId = findFirstBot()
        }
        botId?.let { this.botId = it }

        greyboxPos = tag.getIntArrayOrNull("Bound-Greybox")?.let {
            if (it.size < 3) return@let null
            BlockPos(it[0], it[1], it[2])
        }
    }

    fun findFirstBot() = Showbiz.addons.firstOrNull()?.bots?.entries?.firstOrNull()?.key

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() =
        ClientboundBlockEntityDataPacket.create(this)!!
}