package com.flooferland.showbiz.registry

import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.entities.PropEntity
import com.flooferland.showbiz.entities.SeatEntity
import com.flooferland.showbiz.utils.Extensions.forceLoad
import com.flooferland.showbiz.utils.rl
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level

@Suppress("unused")
sealed class ModLivingEntities<T : LivingEntity> {
    data object Bot : ModLivingEntities<BotEntity>("bot", ::BotEntity);
    data object Prop : ModLivingEntities<PropEntity>("prop", ::PropEntity);
    data object Floodlight : ModLivingEntities<FloodlightEntity>("floodlight", ::FloodlightEntity);
    data object Seat : ModLivingEntities<SeatEntity>("seat", ::SeatEntity);

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
            ModLivingEntities::class.forceLoad()
        }
    }
}