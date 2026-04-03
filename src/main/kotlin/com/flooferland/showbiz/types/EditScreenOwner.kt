package com.flooferland.showbiz.types

import com.flooferland.showbiz.types.EditScreenMenu
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

abstract interface EditScreenOwner<P> : ExtendedScreenHandlerFactory<P> where P: EditScreenMenu.EditScreenPacketPayload {
    var menuData: EditScreenMenu.EditScreenBuf
}
