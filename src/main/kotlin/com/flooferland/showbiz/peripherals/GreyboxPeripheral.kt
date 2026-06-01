package com.flooferland.showbiz.peripherals

import net.minecraft.server.level.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.entities.GreyboxBlockEntity
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.show.toBitIdArray
import com.flooferland.showbiz.types.BitChartStore
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IPeripheral

class GreyboxPeripheral(val blockEntity: GreyboxBlockEntity) : IPeripheral {
    override fun equals(other: IPeripheral?) = other is GreyboxPeripheral && blockEntity == other.blockEntity
    override fun getType() = "greybox"

    @LuaFunction
    @Throws(LuaException::class)
    public fun setBits(ctx: ILuaContext, bits: Map<*, *>) {
        val bits = bits.values.mapNotNull { (it as? Number)?.toInt() }
        ctx.executeMainThreadTask {
            with(blockEntity.show) {
                data.playing = true
                data.mapping = blockEntity.show.data.mapping ?: BitChartStore.DEFAULT
                data.signal.set(bits.map { it.toBitId() }.toBitIdArray())
                send()
            }
            emptyArray()
        }
    }

    @LuaFunction
    @Throws(LuaException::class)
    public fun hasBit(bitId: Int): Boolean {
        return blockEntity.show.data.signal.frameHas(bitId)
    }

    @LuaFunction
    @Throws(LuaException::class)
    public fun setMapping(ctx: ILuaContext, mapping: String) {
        ctx.executeMainThreadTask {
            blockEntity.show.data.mapping = mapping
            emptyArray()
        }
    }

    @LuaFunction
    @Throws(LuaException::class)
    public fun getRegisteredMappings(ctx: ILuaContext): MethodResult {
        if (blockEntity.level !is ServerLevel) return MethodResult.of(emptyArray<String>())
        return ctx.executeMainThreadTask {
            Showbiz.charts.ids.toTypedArray()
        }
    }
}