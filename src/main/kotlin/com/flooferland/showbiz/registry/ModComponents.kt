package com.flooferland.showbiz.registry

import com.flooferland.showbiz.components.OptionBlockPos
import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.*
import net.minecraft.core.component.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*

sealed class ModComponents<T> {
    data object WandBind : ModComponents<OptionBlockPos>(
        "wand_bind",
        { b -> b.persistent(OptionBlockPos.CODEC).networkSynchronized(OptionBlockPos.STREAM_CODEC) }
    );

    constructor(name: String, builder: (DataComponentType.Builder<T>) -> DataComponentType.Builder<T>) {
        this.id = rl(name)
        this.type = builder(DataComponentType.builder<T>()).build()
        if (!DataGenerator.engaged) {
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, this.id, this.type)
        }
    }

    val id: ResourceLocation
    val type: DataComponentType<T>
}