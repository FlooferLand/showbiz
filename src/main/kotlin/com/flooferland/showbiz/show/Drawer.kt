package com.flooferland.showbiz.show

import com.flooferland.showbiz.show.SignalFrame.Companion.NEXT_DRAWER

// TODO: Write an extension for this and a bit type,
//       so I can write "15.td()" and "15.bd()" for top and botttom drawer respectively
enum class Drawer {
    Top,
    Bottom
    ;

    companion object {
        fun fromBit(bit: Int): Drawer =
            if (bit < NEXT_DRAWER) Drawer.Bottom else Drawer.Top
    }
}
