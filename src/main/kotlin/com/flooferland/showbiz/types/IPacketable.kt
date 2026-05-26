package com.flooferland.showbiz.types

import net.minecraft.network.*

interface IPacketable {
    /** Puts the data into the buffer */
    abstract fun encode(buf: FriendlyByteBuf)

    /** Grabs the data from the buffer */
    abstract fun decode(buf: FriendlyByteBuf)
}