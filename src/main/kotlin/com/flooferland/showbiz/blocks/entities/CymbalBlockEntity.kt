package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.collidepart.CollidePartManager
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class CymbalBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Cymbal.entityType!!, pos, blockState), GeoBlockEntity, ICollidePartInteractable {
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override val collidePartInstance = CollidePartManager.create(this) {
        map("cymbal", CollidePartId.Cymbal)
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        collidePartInstance.tick(level, pos, state)
    }
}