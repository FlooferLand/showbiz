package com.flooferland.showbiz.registry

import com.flooferland.showbiz.entities.PlushEntity
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level

@Suppress("unused")
sealed class ModEntities<T : Entity> {
    data object Plush : ModEntities<PlushEntity>("plush", ::PlushEntity)
    ;

    val id: ResourceLocation
    val key: ResourceKey<EntityType<*>>
    val type: EntityType<T>
    constructor(id: String, factory: EntityFactory<T>) {
        this.id = rl(id)
        this.key = ResourceKey.create(Registries.ENTITY_TYPE, this.id)
        this.type = EntityType.Builder.of({ type, level -> factory.factory(level) }, MobCategory.MISC)
            .build(id)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, this.id, this.type)
    }
    fun interface EntityFactory<T : Entity> {
        fun factory(level: Level): T;
    }

    companion object {
        fun register() {
            ModEntities::class.sealedSubclasses.forEach { it.objectInstance }
        }
    }
}