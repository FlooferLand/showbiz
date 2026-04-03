package com.flooferland.showbiz.registry

import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.resources.*
import net.minecraft.world.inventory.*
import com.flooferland.showbiz.menus.CurtainControllerEditMenu
import com.flooferland.showbiz.menus.ShowParserEditMenu
import com.flooferland.showbiz.menus.SpotlightEditMenu
import com.flooferland.showbiz.network.packets.CurtainControllerEditPacket
import com.flooferland.showbiz.network.packets.ShowParserEditPacket
import com.flooferland.showbiz.network.packets.SpotlightEditPacket
import com.flooferland.showbiz.utils.rl
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType

sealed class ModScreenHandlers<T: AbstractContainerMenu, D: Any> {
    data object ShowParserEdit : ModScreenHandlers<ShowParserEditMenu, ShowParserEditPacket>("show_parser", ::ShowParserEditMenu, ShowParserEditPacket.codec)
    data object SpotlightEdit : ModScreenHandlers<SpotlightEditMenu, SpotlightEditPacket>("spotlight", ::SpotlightEditMenu, SpotlightEditPacket.codec)
    data object CurtainControllerEdit : ModScreenHandlers<CurtainControllerEditMenu, CurtainControllerEditPacket>("curtain_controller", ::CurtainControllerEditMenu, CurtainControllerEditPacket.codec)

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