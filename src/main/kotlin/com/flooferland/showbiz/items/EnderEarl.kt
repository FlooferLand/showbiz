package com.flooferland.showbiz.items

import com.flooferland.showbiz.registry.ModSounds
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ThrownEnderpearl
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class EnderEarl(id: ResourceLocation, properties: Properties) : EnderpearlItem(properties) {
    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack = player.getItemInHand(usedHand)
        val pitch = 1.0f - (level.random.nextFloat() * 0.05f)
        level.playSound(null, player.x, player.y, player.z, ModSounds.EnderEarl.event, SoundSource.NEUTRAL, 1.2f, pitch)
        player.getCooldowns().addCooldown(this, 20)
        if (!level.isClientSide) {
            val pearl = ThrownEnderpearl(level, player)
            pearl.setItem(itemStack)
            pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.5f, 1.0f)
            level.addFreshEntity(pearl)
        }

        itemStack.consume(1, player)
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide())
    }
}