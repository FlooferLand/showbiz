package com.flooferland.showbiz.types

import net.minecraft.core.*
import net.minecraft.util.*
import net.minecraft.world.level.block.state.properties.*

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

        val normal: Vec3i get() = when (this) {
            North -> NORTH_NORMAL
            NorthEast -> NORTH_NORMAL.offset(EAST_NORMAL)
            East -> EAST_NORMAL
            EastSouth -> EAST_NORMAL.offset(SOUTH_NORMAL)
            South -> SOUTH_NORMAL
            SouthWest -> SOUTH_NORMAL.offset(WEST_NORMAL)
            West -> WEST_NORMAL
            WestNorth -> WEST_NORMAL.offset(NORTH_NORMAL)
        }
        val stepX get() = normal.x
        val stepZ get() = normal.z
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

        val NORTH_NORMAL = Vec3i(0, 0, -1)
        val SOUTH_NORMAL = Vec3i(0, 0, 1)
        val WEST_NORMAL = Vec3i(-1, 0, 0)
        val EAST_NORMAL = Vec3i(1, 0, 0)
    }
}