package com.flooferland.showbiz.peripherals

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.blocks.entities.GreyboxBlockEntity
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.show.toBitIdArray
import com.flooferland.showbiz.types.BitChartStore
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.GenericPeripheral
import dan200.computercraft.api.peripheral.PeripheralType
import net.minecraft.server.level.ServerLevel

@Suppress("unused")
class GreyboxPeripheral : GenericPeripheral {
    override fun id() = "$MOD_ID:greybox"
    override fun getType() = PeripheralType.ofType("greybox")

    @LuaFunction(mainThread = true)
    @Throws(LuaException::class)
    public fun setBits(blockEntity: GreyboxBlockEntity, ctx: ILuaContext, bits: Map<*, *>) {
        val bits = bits.values.mapNotNull { (it as? Number)?.toInt() }
        with(blockEntity.show) {
            data.playing = true
            data.mapping = blockEntity.show.data.mapping ?: BitChartStore.DEFAULT
            data.signal.set(bits.map { it.toBitId() }.toBitIdArray())
            send()
        }
    }

    @LuaFunction(mainThread = true)
    @Throws(LuaException::class)
    public fun hasBit(blockEntity: GreyboxBlockEntity, ctx: ILuaContext, bitId: Int): Boolean {
        return blockEntity.show.data.signal.frameHas(bitId)
    }

    @LuaFunction(mainThread = true)
    @Throws(LuaException::class)
    public fun setMapping(blockEntity: GreyboxBlockEntity, ctx: ILuaContext, mapping: String) {
        blockEntity.show.data.mapping = mapping
    }

    @LuaFunction(mainThread = true)
    @Throws(LuaException::class)
    public fun getRegisteredMappings(blockEntity: GreyboxBlockEntity, ctx: ILuaContext): Array<String> {
        if (blockEntity.level !is ServerLevel) return emptyArray<String>()
        return Showbiz.charts.ids.toTypedArray()
    }
}