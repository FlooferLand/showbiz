package com.flooferland.showbiz.menus

import com.flooferland.showbiz.network.packets.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModScreenHandlers
import com.flooferland.showbiz.types.EditScreenMenu

class CurtainControllerEditMenu(containerId: Int, packet: CurtainControllerEditPacket)
    : EditScreenMenu<CurtainControllerEditPacket>(containerId, ModScreenHandlers.CurtainControllerEdit.type, packet)
