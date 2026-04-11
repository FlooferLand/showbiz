package com.flooferland.showbiz.types

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity

abstract class AbstractBotPart(entityType: EntityType<*>, level: Level, val id: BotPartId, val owner: StagedBotBlockEntity?, var size: Vec3?) : Entity(entityType, level) {
    companion object {
        var clientSpawn: ((level: Level, id: BotPartId, owner: StagedBotBlockEntity?, size: Vec3?) -> AbstractBotPart)? = null
    }
}