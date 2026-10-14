package com.flooferland.showbiz.menus

import com.flooferland.showbiz.network.packets.editscreen.ShowBridgeEditPacket
import com.flooferland.showbiz.registry.ModScreenHandlers
import com.flooferland.showbiz.types.EditScreenMenu

class ShowBridgeEditMenu(containerId: Int, packet: ShowBridgeEditPacket)
    : EditScreenMenu<ShowBridgeEditPacket>(containerId, ModScreenHandlers.ShowBridgeEdit.type, packet)
