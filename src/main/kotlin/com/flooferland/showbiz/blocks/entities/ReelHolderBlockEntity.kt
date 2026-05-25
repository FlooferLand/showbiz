package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.modelpart.IModelPartInteractable
import com.flooferland.showbiz.types.modelpart.ModelPartManager
import com.flooferland.showbiz.utils.Sounds
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class ReelHolderBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlocks.ReelHolder.entityType!!, pos, state), Container, IModelPartInteractable, GeoBlockEntity {
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    val inventory = Array<ItemStack>(7) { ItemStack.EMPTY }

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        modelPartInstance.tick(level, pos, state)
    }

    override fun setRemoved() {
        super.setRemoved()
        modelPartInstance.kill()
    }

    override val modelPartInstance = ModelPartManager.create(this, ModBlocks.ReelHolder)
    override fun getInteractionMapping() = hashMapOf<String, Int>().also { map ->
        (0..6).forEach { map["reels/reel$it"] = it }
    }

    override fun getNameMapping() = hashMapOf<Int, String>().also { map ->
        (0..6).forEach { map[it] = if (getItem(it).isEmpty) "Empty" else getItem(it).displayName.string }
    }

    override fun onInteract(key: Int, level: Level, player: Player) {
        val slot = key
        if (slot !in 0 until containerSize) return

        val currentStack = getItem(slot)
        val hand = player.usedItemHand ?: InteractionHand.MAIN_HAND
        val heldItem = player.getItemInHand(hand)

        if (!currentStack.isEmpty && heldItem.item is ReelItem) {
            player.setItemInHand(hand, currentStack)
            setItem(slot, heldItem)
        } else if (currentStack.isEmpty && heldItem.item is ReelItem) {
            setItem(slot, heldItem)
            player.setItemInHand(hand, ItemStack.EMPTY)
        } else if (!currentStack.isEmpty && heldItem.isEmpty) {
            player.setItemInHand(hand, currentStack)
            setItem(slot, ItemStack.EMPTY)
        }

        Sounds.play(player, ModSounds.ReelPlay)
    }

    // region | Container
    override fun getContainerSize() = inventory.size
    override fun getMaxStackSize() = 1
    override fun isEmpty() = inventory.all { it.isEmpty }
    override fun getItem(slot: Int) = inventory.getOrElse(slot) { ItemStack.EMPTY!! }
    override fun canPlaceItem(slot: Int, stack: ItemStack) = (slot < containerSize) && stack.item is ReelItem
    override fun setItem(slot: Int, stack: ItemStack) {
        if (!canPlaceItem(slot, stack)) return
        inventory[slot] = stack
        setChanged()
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack {
        if (slot > containerSize) return ItemStack.EMPTY
        if (amount == 0) return ItemStack.EMPTY
        val item = inventory[slot].copy()
        inventory[slot] = ItemStack.EMPTY
        setChanged()
        return item
    }
    override fun removeItemNoUpdate(slot: Int) = removeItem(slot, 1)
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)
    override fun clearContent() {
        inventory.fill(ItemStack.EMPTY)
        setChanged()
    }
    // endregion
}