package com.flooferland.showbiz.items

import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.entity.projectile.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import com.flooferland.showbiz.registry.ModSounds

class EnderEarl(properties: Properties) : EnderpearlItem(properties) {
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