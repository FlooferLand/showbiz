package com.flooferland.showbiz.types.connection

object ChannelRegistry {
    public val channels = mutableMapOf<String, DataChannelOut<*>>()
    fun get(id: String) = channels[id]
    fun registerOut(channel: DataChannelOut<*>) {
        channels[channel.id] = channel
    }
}
