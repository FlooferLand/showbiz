package com.flooferland.showbiz.registry

import com.flooferland.showbiz.components.OptionBlockPos
import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.utils.rl
import com.mojang.serialization.Codec
import net.minecraft.core.*
import net.minecraft.core.component.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*

sealed class ModComponents<T> {
    data object WandBind : ModComponents<OptionBlockPos>(
        "wand_bind",
        { b -> b.persistent(OptionBlockPos.CODEC).networkSynchronized(OptionBlockPos.STREAM_CODEC) }
    )
    data object FileName : ModComponents<String>(
        "filename",
        { b -> b.persistent(Codec.STRING) }
    )
    ;

    constructor(name: String, builder: (DataComponentType.Builder<T>) -> DataComponentType.Builder<T>) {
        this.id = rl(name)
        this.type = builder(DataComponentType.builder<T>()).build()
        if (!DataGenerator.engaged) {
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, this.id, this.type)
        }
    }

    val id: ResourceLocation
    val type: DataComponentType<T>

    companion object {
        init { ModComponents::class.sealedSubclasses.forEach { it.objectInstance } }
    }
}