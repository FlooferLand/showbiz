package com.flooferland.showbiz.registry

/*sealed class ModDecoEntities<T : LivingEntity> {
    object Moonshine : ModDecoEntities<MoonshinePropEntity>("moonshine", ::MoonshinePropEntity)
    ;

    val id: ResourceLocation
    val key: ResourceKey<EntityType<*>>
    val type: EntityType<T>
    val item: FancyItem
    constructor(id: String, factory: EntityFactory<T>) {
        this.id = rl(id)
        this.key = ResourceKey.create(Registries.ENTITY_TYPE, this.id)
        this.type = EntityType.Builder.of({ _, level -> factory.factory(level) }, MobCategory.MISC).build(id)
        this.item = DecoEntityItem(this.id, Item.Properties())
        entries += this
    }

    fun register() {
        // Entity
        Registry.register(BuiltInRegistries.ENTITY_TYPE, this.id, this.type)
        FabricDefaultAttributeRegistry.register(type, LivingEntity.createLivingAttributes())

        // Item
        Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(BuiltInRegistries.ITEM.key(), id), item)
    }

    companion object {
        val entries = mutableSetOf<ModDecoEntities<*>>()

        fun register() {
            ModDecoEntities::class.forceLoad()
            for (entry in entries) entry.register()
        }
    }
}*/
