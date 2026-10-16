package com.flooferland.showbiz.show

import net.minecraft.network.chat.*
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

        /** Converts a global bit id into a local bit (ex: "16 TD") */
        fun toLocalBit(bit: BitId): Pair<BitId, Drawer> =
            Pair(
                if (bit > BitUtils.NEXT_DRAWER) (bit - BitUtils.NEXT_DRAWER).toUShort() else bit,
                Drawer.fromBit(bit)
            )

        /** Formats a global bit id into a string (ex: "16 TD") */
        fun formatBit(bit: BitId): String {
            val localBit = toLocalBit(bit)
            return "${localBit.first} ${localBit.second.toStringDrawer()}"
        }

        /** Formats a global bit id into a text component (ex: "16 TD") */
        fun formatBitAsComp(bit: BitId): MutableComponent {
            val localBit = toLocalBit(bit)
            return Component.literal(localBit.first.toString())
                .append(" ")
                .append(localBit.second.toCompDrawer())
        }
    }
}
