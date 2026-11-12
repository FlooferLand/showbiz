package com.flooferland.showbiz.registry

import com.flooferland.showbiz.components.FloodlightComponent
import com.flooferland.showbiz.components.OptionBlockPos
import com.flooferland.showbiz.components.PlushComponent
import com.flooferland.showbiz.components.PropComponent
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.utils.rl
import com.mojang.serialization.Codec
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.ResourceLocation

sealed class ModComponents<T> {
    data object BlockOwner : ModComponents<OptionBlockPos>(
        "block_owner",
        { b -> b.persistent(OptionBlockPos.CODEC).networkSynchronized(OptionBlockPos.STREAM_CODEC) }
    )
    data object HeldConnection : ModComponents<OwnerId>(
        "held_connection",
        { b -> b.persistent(OwnerId.CODEC).networkSynchronized(OwnerId.STREAM_CODEC) }
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
    data object Prop : ModComponents<PropComponent>(
        "prop",
        { b -> b.persistent(PropComponent.CODEC).networkSynchronized(PropComponent.STREAM_CODEC) }
    )
    data object Floodlight : ModComponents<FloodlightComponent>(
        "floodlight",
        { b -> b.persistent(FloodlightComponent.CODEC).networkSynchronized(FloodlightComponent.STREAM_CODEC) }
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