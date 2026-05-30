package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import com.flooferland.showbiz.blocks.PlushBlock
import com.flooferland.showbiz.entities.PlushEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.getOrNull
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.jvm.optionals.getOrNull

// TODO: Migrated to PlushEntity. Should be removed.
class PlushBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Plush.entityType, pos, blockState), GeoBlockEntity {
    val geckoCache = GeckoLibUtil.createInstanceCache(this)!!
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geckoCache

    val defaultItem = ModBlocks.Plush.item.defaultInstance!!
    var itemStack: ItemStack = defaultItem

    var migrated = false

    // Migration
    fun tick(level: Level, pos: BlockPos, blockState: BlockState) {
        if (migrated || isRemoved) return
        if (level.isClientSide) return
        val pos = blockPos
        val stack = itemStack.copyWithCount(1)
        val segment = blockState.getValue(PlushBlock.ROTATION)
        level.removeBlock(blockPos, false)

        val entity = PlushEntity(level)
        entity.setPos(pos.bottomCenter.add(0.0, PlushBlock.getSittingOffset(level, pos), 0.0))
        entity.yRot = (360f - RotationSegment.convertToDegrees(segment)) % 360f
        entity.itemStack = stack
        level.addFreshEntity(entity)
        migrated = true
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        if (itemStack != ItemStack.EMPTY)
            itemStack.save(registries)?.let { tag.put("item", it) }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        itemStack = tag.getOrNull("item")?.let { ItemStack.parse(registries, it).getOrNull() } ?: defaultItem
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }


    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!
}