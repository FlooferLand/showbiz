package com.flooferland.showbiz.resources

import net.minecraft.server.packs.resources.*
import net.minecraft.util.profiling.*
import net.minecraft.world.phys.Vec3
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.ModelPart
import com.flooferland.showbiz.types.ModelPartManager
import com.flooferland.showbiz.types.ModelPartMap
import com.flooferland.showbiz.types.Vec3f
import com.flooferland.showbiz.utils.Extensions.divide
import com.flooferland.showbiz.utils.ShowbizUtils
import com.flooferland.showbiz.utils.rl
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import org.joml.Vector3d
import software.bernie.geckolib.cache.`object`.GeoBone

object ModelPartReloadListener : SimplePreparableReloadListener<ModelPartMap>(), IdentifiableResourceReloadListener {
    override fun getFabricId() = rl("modelpart")

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): ModelPartMap {
        val map = ModelPartMap()

        Showbiz.log.info("Preparing ${ModelPartReloadListener::class.simpleName}")
        val resources = manager.listResources("geo/block") { _ -> true }

        for ((id, res) in resources) {
            if (!id.path.endsWith(".geo.json")) continue
            val id = id.withPath(id.path.replace("geo/block/", "").replace(".geo.json", ""))
            val model = run {
                val reader = res.openAsReader()
                ShowbizUtils.loadBakedModel(id, reader.readText().also { reader.close() })
            } ?: continue
            val partMap = map.getOrPut(id) { hashMapOf() }

            fun addBone(bone: GeoBone, posOffset: Vec3? = null) {
                bone.childBones.forEach { addBone(it, Vec3(bone.posX.toDouble(), bone.posY.toDouble(), bone.posZ.toDouble()).add(posOffset ?: Vec3.ZERO)) }
                for ((i, cube) in bone.cubes.withIndex()) {
                    val part = ModelPart(
                        cube.pivot.divide(2.0).add(posOffset ?: Vec3.ZERO),
                        cube.size,
                        cube.pivot,
                        cube.rotation.let { Vector3d(it.x, it.y, it.z) }
                    )
                    partMap["${bone.name}/$i"] = part
                }

                val size = runCatching { bone.cubes.map { it.size }.reduce { acc, size -> acc.add(size) } }.getOrNull() ?: Vec3(0.1, 0.1, 0.1)
                val part = ModelPart(
                    Vec3(bone.posX.toDouble(), bone.posY.toDouble(), bone.posZ.toDouble()),
                    size,
                    bone.let { Vec3(it.pivotX.toDouble(), it.pivotY.toDouble(), it.pivotZ.toDouble()) },
                    bone.rotationVector
                )
                partMap[bone.name] = part
            }
            model.topLevelBones.forEach { addBone(it) }
        }
        return map
    }

    override fun apply(data: ModelPartMap, resourceManager: ResourceManager, profiler: ProfilerFiller) {
        ModelPartManager.modelPartData = data
    }
}