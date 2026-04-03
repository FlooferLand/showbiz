package com.flooferland.showbiz.menus

import com.flooferland.showbiz.network.packets.SpotlightEditPacket
import com.flooferland.showbiz.registry.ModScreenHandlers
import com.flooferland.showbiz.types.EditScreenMenu

class SpotlightEditMenu(containerId: Int, packet: SpotlightEditPacket)
    : EditScreenMenu<SpotlightEditPacket>(containerId, ModScreenHandlers.SpotlightEdit.type, packet)
