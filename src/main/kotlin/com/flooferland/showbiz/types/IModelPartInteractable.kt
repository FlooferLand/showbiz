package com.flooferland.showbiz.types

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

interface IModelPartInteractable {
    val modelPartInstance: ModelPartManager.IInstance
    fun getInteractionMapping(): Map<String, Int>
    fun onInteract(key: Int, level: Level, player: Player)
}