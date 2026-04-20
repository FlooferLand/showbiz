package com.flooferland.showbiz.show

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import com.flooferland.bizlib.bits.BitUtils

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
            style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("$name drawer")))
        }!!

    fun toStringEnglish() = when (this) {
        Top -> "top"
        Bottom -> "bottom"
    }

    @Deprecated("Use the more verbose toString variants instead")
    override fun toString() = toStringEnglish()

    companion object {
        /** Returns the drawer of the global bit id */
        fun fromBit(bit: BitId): Drawer =
            if (bit < BitUtils.NEXT_DRAWER) Drawer.Top else Drawer.Bottom

        /** Formats a global bit id into a string (ex: "16 TD") */
        fun formatBit(bit: BitId): String =
            "${if (bit > BitUtils.NEXT_DRAWER) bit - BitUtils.NEXT_DRAWER else bit} ${Drawer.fromBit(bit).toStringDrawer()}"

        /** Formats a global bit id into a text component (ex: "16 TD") */
        fun formatBitAsComp(bit: BitId): MutableComponent =
            Component.literal((if (bit > BitUtils.NEXT_DRAWER) bit - BitUtils.NEXT_DRAWER else bit).toString())
                .append(" ")
                .append(Drawer.fromBit(bit).toCompDrawer())
    }
}
