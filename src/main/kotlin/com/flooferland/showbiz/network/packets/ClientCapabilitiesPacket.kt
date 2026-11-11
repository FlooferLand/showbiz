package com.flooferland.showbiz.network.packets

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlIndentation
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModConfig
import com.flooferland.showbiz.types.SemVer
import com.flooferland.showbiz.utils.rl
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class ClientCapabilitiesPacket(val config: ModConfig, val addons: Map<String, SemVer?>) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val toml = Toml(
            inputConfig = TomlInputConfig.compliant(ignoreUnknownNames = true),
            outputConfig = TomlOutputConfig.compliant(TomlIndentation.NONE, ignoreDefaultValues = true),
        )

        val type = CustomPacketPayload.Type<ClientCapabilitiesPacket>(rl("client_capabilities"))
        val codec = StreamCodec.of<FriendlyByteBuf, ClientCapabilitiesPacket>(
            { buf, packet ->
                // Saving the config
                val config = runCatching { toml.encodeToString(packet.config) }.getOrNull()
                buf.writeBoolean(config != null)
                if (config != null) buf.writeUtf(config)

                // Saving the addon list
                buf.writeMap(
                    packet.addons,
                    { buf, key ->
                        buf.writeUtf(key)
                    },
                    { buf, value ->
                        buf.writeBoolean(value != null)
                        if (value != null) buf.writeUtf(value.toString())
                    }
                )
            },
            { buf ->
                val config = if (buf.readBoolean()) toml.decodeFromString<ModConfig>(buf.readUtf()) else null
                ClientCapabilitiesPacket(
                    config = config ?: Showbiz.config.clone(),
                    addons = buf.readMap({ it.readUtf() }, { buf -> if (buf.readBoolean()) SemVer(buf.readUtf()) else null })
                )
            }
        )!!
    }
}