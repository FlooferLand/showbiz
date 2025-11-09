package com.flooferland.showbiz.addons.data

import com.flooferland.showbiz.utils.Vec3f
import software.bernie.geckolib.cache.`object`.BakedGeoModel

data class BotModelData(
    val bakedModel: BakedGeoModel,
    val initBoneRots: MutableMap<String, Vec3f>,
    val initBoneMoves: MutableMap<String, Vec3f>
)