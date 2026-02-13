package com.flooferland.showbiz.types

import net.minecraft.util.StringRepresentable
import net.minecraft.world.level.block.state.properties.EnumProperty

class GigaDirectionProperty(name: String) : EnumProperty<GigaDirectionProperty.Enum>(name, Enum::class.java, values) {
    enum class Enum(val id: String, val angle: Float) : StringRepresentable {
        North("north", 0f),
        NorthEast("north_east", 45f),
        East("east", 90f),
        EastSouth("east_south", 135f),
        South("south", 180f),
        SouthWest("south_west", 225f),
        West("west", 270f),
        WestNorth("west_north", 315f)
        ;
        override fun getSerializedName() = id
    }

    companion object {
        val values = listOf(
            Enum.North,
            Enum.NorthEast,
            Enum.East,
            Enum.EastSouth,
            Enum.South,
            Enum.SouthWest,
            Enum.West,
            Enum.WestNorth
        )
    }
}