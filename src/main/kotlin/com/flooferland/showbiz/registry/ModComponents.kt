package com.flooferland.showbiz.registry

import net.minecraft.core.*
import net.minecraft.core.component.*
import net.minecraft.core.registries.*
import net.minecraft.network.codec.*
import net.minecraft.resources.*
import com.flooferland.showbiz.components.OptionBlockPos
import com.flooferland.showbiz.components.PlushComponent
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.types.connection.ConnectionOwnerId
import com.flooferland.showbiz.utils.rl
import com.mojang.serialization.Codec

sealed class ModComponents<T> {
    data object BlockOwner : ModComponents<OptionBlockPos>(
        "block_owner",
        { b -> b.persistent(OptionBlockPos.CODEC).networkSynchronized(OptionBlockPos.STREAM_CODEC) }
    )
    data object HeldConnection : ModComponents<ConnectionOwnerId>(
        "held_connection",
        { b -> b.persistent(ConnectionOwnerId.CODEC).networkSynchronized(ConnectionOwnerId.STREAM_CODEC) }
    )
    data object FileName : ModComponents<String>(
        "filename",
        { b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8) }
    )
    data object Plush : ModComponents<PlushComponent>(
        "plush",
        { b -> b.persistent(PlushComponent.CODEC).networkSynchronized(PlushComponent.STREAM_CODEC) }
    )
    data object BotId : ModComponents<ResourceId>(
        "bot",
        { b -> b.persistent(ResourceId.CODEC).networkSynchronized(ResourceId.STREAM_CODEC) }
    )
    ;

    constructor(name: String, builder: (DataComponentType.Builder<T>) -> DataComponentType.Builder<T>) {
        this.id = rl(name)
        this.type = builder(DataComponentType.builder<T>()).build()
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, this.id, this.type)
    }

    val id: ResourceLocation
    val type: DataComponentType<T>

    companion object {
        fun register() {
            ModComponents::class.sealedSubclasses.forEach { it.objectInstance }
        }
    }
}