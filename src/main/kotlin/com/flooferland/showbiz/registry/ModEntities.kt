package com.flooferland.showbiz.registry

import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.entities.PlushEntity
import com.flooferland.showbiz.utils.rl

@Suppress("unused")
sealed class ModEntities<T : Entity> {
    data object Plush : ModEntities<PlushEntity>("plush", ::PlushEntity);
    data object Bot : ModEntities<BotEntity>("bot", ::BotEntity);

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