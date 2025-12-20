package com.flooferland.showbiz.show

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import com.flooferland.showbiz.show.SignalFrame.Companion.NEXT_DRAWER

// TODO: Write an extension for this and a bit type,
//       so I can write "15.td()" and "15.bd()" for top and botttom drawer respectively
enum class Drawer {
    Top,
    Bottom
    ;

    /** Returns either a 'TD' or 'BD' string */
    fun toStringDrawer() = when (this) {
        Top -> "TD"
        Bottom -> "BD"
    }

    /** Similar to [toStringDrawer] but adds a neat hover thingy */
    fun toCompDrawer() = Component.literal(toStringDrawer())
        .withStyle { style ->
            style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("${toStringEnglish().capitalize()} drawer")))
        }

    fun toStringEnglish() = when (this) {
        Top -> "top"
        Bottom -> "bottom"
    }

    @Deprecated("Use the more verbose toString variants instead")
    override fun toString() = toStringDrawer()

    companion object {
        fun fromBit(bit: BitId): Drawer =
            if (bit < NEXT_DRAWER) Drawer.Bottom else Drawer.Top
    }
}
