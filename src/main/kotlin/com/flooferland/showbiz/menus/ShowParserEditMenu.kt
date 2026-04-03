package com.flooferland.showbiz.menus

import com.flooferland.showbiz.network.packets.ShowParserEditPacket
import com.flooferland.showbiz.registry.ModScreenHandlers
import com.flooferland.showbiz.types.EditScreenMenu

class ShowParserEditMenu(containerId: Int, packet: ShowParserEditPacket)
    : EditScreenMenu<ShowParserEditPacket>(containerId, ModScreenHandlers.ShowParserEdit.type, packet)
