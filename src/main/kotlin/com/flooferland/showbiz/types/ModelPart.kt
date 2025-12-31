package com.flooferland.showbiz.types

import net.minecraft.resources.*
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d

class ModelPartMap() : HashMap<ResourceLocation, HashMap<String, ModelPart>>()

data class ModelPart(val pos: Vec3, val size: Vec3?, val pivot: Vec3, val rotation: Vector3d, val parent: String? = null)
