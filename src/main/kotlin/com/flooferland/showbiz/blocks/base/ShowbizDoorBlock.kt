package com.flooferland.showbiz.blocks.base

import net.minecraft.world.level.block.*
import com.flooferland.showbiz.registry.ModSetTypes

class ShowbizDoorBlock(properties: Properties) : DoorBlock(ModSetTypes.ShowbizWood.type, properties.noOcclusion()) {}
