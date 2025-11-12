package com.flooferland.showbiz.types.connection

open class DataChannelIn<T>(val id: String)
open class DataChannelOut<T>(val id: String) {
    init { ChannelRegistry.registerOut(this) }
}
