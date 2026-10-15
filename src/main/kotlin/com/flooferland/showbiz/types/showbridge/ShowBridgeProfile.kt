package com.flooferland.showbiz.types.showbridge

import net.minecraft.nbt.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import com.flooferland.showbiz.types.ICompoundable
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import kotlin.jvm.optionals.getOrNull

data class ShowBridgeProfile(
    var enabled: Boolean = false,
    var method: ShowBridgeMethod = ShowBridgeMethod.TextList
) : ICompoundable {
    var bitFilter = MappedBits()

    val textList = ShowBridgeSettings.TextList()
    val binaryList = ShowBridgeSettings.BinaryList()

    override fun save(tag: CompoundTag) {
        tag.putBoolean("enabled", enabled)
        tag.putInt("method", method.ordinal)
        MappedBits.CODEC.encodeStart(NbtOps.INSTANCE, bitFilter).result().getOrNull()?.let {
            tag.put("bit_filter", it)
        }

        textList.save(tag)
        binaryList.save(tag)
    }

    override fun load(tag: CompoundTag) {
        enabled = tag.getBooleanOrNull("enabled") ?: false
        tag.getIntOrNull("method")?.let { method = ShowBridgeMethod.entries.getOrNull(it) ?: ShowBridgeMethod.TextList }
        tag.get("bit_filter")?.let { filterTag ->
            MappedBits.CODEC.parse(NbtOps.INSTANCE, filterTag).result().getOrNull()?.let { loaded ->
                loaded.charts.forEach { chartId ->
                    loaded.getOrPutDefault(chartId).forEach { bitFilter.addBit(chartId, it) }
                }
            }
        }

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