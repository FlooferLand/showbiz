package com.flooferland.showbiz.types

import com.flooferland.showbiz.utils.Extensions.voxelSnap
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.round

interface IStackPlacePreview {
    fun getEntity(level: Level, stack: ItemStack): Entity
    fun placeEntity(entity: Entity, pos: Vec3, player: Player) {
        var pos = pos.voxelSnap()
        var yaw = (player.yRot + 180f)

        if (player.isCrouching) {
            pos = pos.voxelSnap(4.0)
            yaw = round(yaw / 45f) * 45f
        } else {
            yaw = round(yaw / 15f) * 15f
        }

        entity.setPos(pos)
        entity.xRot = 0f
        entity.yRot = yaw
        entity.yRotO = yaw
        entity.yHeadRot = yaw
        if (entity is LivingEntity) {
            entity.yHeadRotO = yaw
            entity.yBodyRot = yaw
            entity.yBodyRotO = yaw
        }
    }
}