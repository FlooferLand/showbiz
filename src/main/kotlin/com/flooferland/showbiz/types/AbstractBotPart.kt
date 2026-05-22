package com.flooferland.showbiz.types

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity

abstract class AbstractBotPart(entityType: EntityType<*>, level: Level, val id: InteractPartId, val owner: StagedBotBlockEntity?) : Entity(entityType, level) {
    companion object {
        var clientSpawn: ((level: Level, id: InteractPartId, owner: StagedBotBlockEntity?) -> AbstractBotPart)? = null
    }
}