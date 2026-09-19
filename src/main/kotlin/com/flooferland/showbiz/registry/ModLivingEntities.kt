package com.flooferland.showbiz.registry

import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.utils.rl
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry

@Suppress("unused")
sealed class ModLivingEntities<T : LivingEntity> {
    data object Bot : ModLivingEntities<BotEntity>("bot", ::BotEntity);
    data object Floodlight : ModLivingEntities<FloodlightEntity>("floodlight", ::FloodlightEntity);

    val id: ResourceLocation
    val key: ResourceKey<EntityType<*>>
    val type: EntityType<T>
    constructor(id: String, factory: EntityFactory<T>) {
        this.id = rl(id)
        this.key = ResourceKey.create(Registries.ENTITY_TYPE, this.id)
        this.type = EntityType.Builder.of({ type, level -> factory.factory(level) }, MobCategory.MISC)
            .build(id)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, this.id, this.type)
        FabricDefaultAttributeRegistry.register(type, LivingEntity.createLivingAttributes())
    }
    fun interface EntityFactory<T : Entity> {
        fun factory(level: Level): T;
    }

    companion object {
        fun register() {
            ModLivingEntities::class.sealedSubclasses.forEach { it.objectInstance }
        }
    }
}