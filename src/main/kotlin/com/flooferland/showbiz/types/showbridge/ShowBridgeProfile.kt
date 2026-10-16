package com.flooferland.showbiz.types.showbridge

import net.minecraft.nbt.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import com.flooferland.showbiz.types.ICompoundable
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getEnumOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import com.flooferland.showbiz.utils.Extensions.putEnum
import kotlin.jvm.optionals.getOrNull

data class ShowBridgeProfile(
    var enabled: Boolean = false,
    var stayOpen: Boolean = true,
    var method: ShowBridgeMethod = ShowBridgeMethod.TextList
) : ICompoundable {
    var bitFilter = MappedBits()
    var serialPort = ""
    var serialBaudRate = 19200

    val textList = ShowBridgeSettings.TextList()
    val binaryList = ShowBridgeSettings.BinaryList()

    var output: String = ""

    override fun save(tag: CompoundTag) {
        tag.putBoolean("enabled", enabled)
        tag.putBoolean("stay_open", stayOpen)
        tag.putEnum("method", method)
        MappedBits.CODEC.encodeStart(NbtOps.INSTANCE, bitFilter).result().getOrNull()?.let {
            tag.put("bit_filter", it)
        }
        tag.putString("serial_port", serialPort)
        tag.putInt("serial_rate", serialBaudRate)

        tag.putString("output", output)

        textList.save(tag)
        binaryList.save(tag)
    }

    override fun load(tag: CompoundTag) {
        enabled = tag.getBooleanOrNull("enabled") ?: false
        stayOpen = tag.getBooleanOrNull("stay_open") ?: false
        tag.getEnumOrNull<ShowBridgeMethod>("method")?.let { method = it }
        tag.get("bit_filter")?.let { filterTag ->
            MappedBits.CODEC.parse(NbtOps.INSTANCE, filterTag).result().getOrNull()?.let { loaded ->
                loaded.charts.forEach { chartId ->
                    loaded.getOrPutDefault(chartId).forEach { bitFilter.addBit(chartId, it) }
                }
            }
        }
        tag.getStringOrNull("serial_port")?.let { serialPort = it }
        tag.getIntOrNull("serial_rate")?.let { serialBaudRate = it }

        tag.getStringOrNull("output")?.let { output = it }

        textList.load(tag)
        binaryList.load(tag)
    }

    companion object {
        val codec = StreamCodec.of<FriendlyByteBuf, ShowBridgeProfile>(
            { buf, conf -> buf.writeNbt(CompoundTag().also { conf.save(it) }) },
            { buf -> ShowBridgeProfile().also { it.load(buf.readNbt()!!) } }
        )!!
    }
}