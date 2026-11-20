package com.flooferland.showbiz.registry

import com.flooferland.showbiz.menus.*
import com.flooferland.showbiz.network.packets.BitViewPacket
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.network.packets.editscreen.*
import com.flooferland.showbiz.utils.Extensions.forceLoad
import com.flooferland.showbiz.utils.rl
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType

// Don't forget to also register the screen in ShowbizClient !!
sealed class ModScreenHandlers<T: AbstractContainerMenu, D: Any> {
    data object ShowParserEdit : ModScreenHandlers<ShowParserEditMenu, ShowParserEditPacket>("show_parser", ::ShowParserEditMenu, ShowParserEditPacket.codec)
    data object ShowBridgeEdit : ModScreenHandlers<ShowBridgeEditMenu, ShowBridgeEditPacket>("show_bridge", ::ShowBridgeEditMenu, ShowBridgeEditPacket.codec)
    data object SpotlightEdit : ModScreenHandlers<SpotlightEditMenu, SpotlightEditPacket>("spotlight", ::SpotlightEditMenu, SpotlightEditPacket.codec)
    data object FloodlightEdit : ModScreenHandlers<FloodlightEditMenu, FloodlightEditPacket>("floodlight", ::FloodlightEditMenu, FloodlightEditPacket.codec)
    data object CurtainControllerEdit : ModScreenHandlers<CurtainControllerEditMenu, CurtainControllerEditPacket>("curtain_controller", ::CurtainControllerEditMenu, CurtainControllerEditPacket.codec)
    data object BotSelect : ModScreenHandlers<BotSelectMenu, BotListSelectPacket>("bot_select", ::BotSelectMenu, BotListSelectPacket.codec)
    data object BitView : ModScreenHandlers<BitViewMenu, BitViewPacket>("bit_view", ::BitViewMenu, BitViewPacket.codec)

    val id: ResourceLocation
    val type: MenuType<T>
    constructor(id: String, factory: (Int, D) -> T, codec: StreamCodec<FriendlyByteBuf, D>) {
        this.id = rl(id)
        this.type = Registry.register(BuiltInRegistries.MENU, this.id, ExtendedScreenHandlerType({ i, _, data -> factory(i, data) }, codec))
    }

    companion object {
        fun register() {
            ModScreenHandlers::class.forceLoad()
        }
    }
}