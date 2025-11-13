package com.flooferland.showbiz.types.connection

import com.flooferland.showbiz.show.SignalFrame

object ChannelRegistry {
    public val channels = mutableMapOf<String, DataChannelOut<*>>()
    fun get(id: String) = channels[id]
    fun registerOut(channel: DataChannelOut<*>) {
        channels[channel.id] = channel
    }
}

/**
 * Collection of ports for the built-in port/channels in the Showbiz mod.
 * You are free to make your own object for storing these if you're making an addon for this mod.
 * Naming convention could be (AddonName)Ports. (ex: ShowbizPorts)
 */
object Ports {
    object PlayingOut : DataChannelOut<Boolean>("playing")
    object PlayingIn : DataChannelIn<Boolean>("playing")

    object SignalOut : DataChannelOut<SignalFrame>("signal")
    object SignalIn : DataChannelIn<SignalFrame>("signal")
}
