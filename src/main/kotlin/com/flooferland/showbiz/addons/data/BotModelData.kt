package com.flooferland.showbiz.addons.data

import com.flooferland.showbiz.types.math.Vec3fc
import software.bernie.geckolib.cache.`object`.BakedGeoModel

data class BotModelData(
    val bakedModel: BakedGeoModel,
    val initBoneRots: MutableMap<String, Vec3fc>,
    val initBoneMoves: MutableMap<String, Vec3fc>
)