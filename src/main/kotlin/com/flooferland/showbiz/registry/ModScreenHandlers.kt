package com.flooferland.showbiz.registry

import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.resources.*
import net.minecraft.world.inventory.*
import com.flooferland.showbiz.menus.ShowParserMenu
import com.flooferland.showbiz.network.packets.ShowParserDataPacket
import com.flooferland.showbiz.utils.rl
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType

sealed class ModScreenHandlers<T: AbstractContainerMenu, D: Any> {
    data object ShowParser : ModScreenHandlers<ShowParserMenu, ShowParserDataPacket>("show_parser", ::ShowParserMenu, ShowParserDataPacket.codec)

    val id: ResourceLocation
    val type: MenuType<T>
    constructor(id: String, factory: (Int, D) -> T, codec: StreamCodec<FriendlyByteBuf, D>) {
        this.id = rl(id)
        this.type = Registry.register(BuiltInRegistries.MENU, this.id, ExtendedScreenHandlerType({ i, _, data -> factory(i, data) }, codec))
    }

    companion object {
        init { ModScreenHandlers::class.sealedSubclasses.forEach { it.objectInstance } }
    }
}