package com.flooferland.showbiz.menus

import com.flooferland.showbiz.network.packets.FloodlightEditPacket
import com.flooferland.showbiz.registry.ModScreenHandlers
import com.flooferland.showbiz.types.EditScreenMenu

class FloodlightEditMenu(containerId: Int, packet: FloodlightEditPacket)
    : EditScreenMenu<FloodlightEditPacket>(containerId, ModScreenHandlers.FloodlightEdit.type, packet)
