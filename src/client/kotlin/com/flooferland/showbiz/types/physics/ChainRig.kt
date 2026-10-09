package com.flooferland.showbiz.types.physics

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.addons.data.ChainLayout
import com.flooferland.showbiz.utils.Extensions.getAllBones
import com.mojang.blaze3d.platform.NativeImage
import org.joml.Matrix4fc
import org.joml.Vector3f
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.loading.json.raw.Bone
import software.bernie.geckolib.loading.json.raw.Cube
import software.bernie.geckolib.loading.json.raw.FaceUV
import software.bernie.geckolib.loading.json.raw.MinecraftGeometry
import software.bernie.geckolib.loading.json.raw.Model
import software.bernie.geckolib.loading.json.raw.UVFaces
import software.bernie.geckolib.loading.json.raw.UVUnion
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object ChainRig {
    const val LINK_MARK: String = "__link"

    fun transform(texture: ByteArray, tables: Map<String, ChainSolver.Params>, label: String): ((Model) -> Model)? {
        val alpha = Alpha.read(texture) ?: return null
        return { model ->
            val out = expand(model, alpha, tables)
            val names = out.minecraftGeometry()?.firstOrNull()?.bones()?.mapNotNull { it.name() }?.filter { it.contains(LINK_MARK) }.orEmpty()
            for (pattern in tables.keys) {
                val links = names.count { name -> tables.keys.firstOrNull { matches(it, name.substringBefore(LINK_MARK)) } == pattern }
                Showbiz.log.info("Chains for '$label': $links links from [chains.\"$pattern\"]")
            }
            out
        }
    }

    // every chain of link bones in [model], knot to tip. Empty for a model without chains
    fun layout(model: BakedGeoModel): ChainLayout {
        val chains = LinkedHashMap<String, MutableList<Pair<Int, GeoBone>>>()
        for (bone in model.getAllBones().sortedBy { it.name }) {
            if (!bone.name.contains(LINK_MARK)) continue
            val row = bone.name.substringAfterLast('_').toIntOrNull() ?: continue
            chains.getOrPut(bone.name.substringBeforeLast('_')) { ArrayList() } += row to bone
        }
        if (chains.isEmpty()) return ChainLayout.EMPTY
        val ids = chains.keys.toList()
        val links = Array(ids.size) { i -> chains[ids[i]]!!.sortedBy { it.first }.map { it.second.name }.toTypedArray() }
        val roots = Array(ids.size) { i -> chains[ids[i]]!!.minBy { it.first }.second.parent?.name ?: ids[i].substringBefore(LINK_MARK) }
        return ChainLayout(links, roots, Array(ids.size) { ids[it].substringBefore(LINK_MARK) }, IntArray(ids.size) { ids[it].hashCode() })
    }

    // A chain's length as drawn from its links' pivots; the last link repeats the one above it
    fun length(links: Array<String>, poses: Map<String, Matrix4fc>): Float {
        val a = Vector3f()
        val b = Vector3f()
        var total = 0f
        var fallback = 0.05f
        for (k in links.indices) {
            val pa = poses[links[k]]
            val pb = if (k + 1 < links.size) poses[links[k + 1]] else null
            val d = if (pa != null && pb != null) a.set(pa.m30(), pa.m31(), pa.m32()).distance(b.set(pb.m30(), pb.m31(), pb.m32())) else fallback
            if (d > 1e-5f) fallback = d
            total += d
        }
        if (total < 1e-4f) total = 0.05f * links.size
        return total
    }

    private fun expand(model: Model, alpha: Alpha, tables: Map<String, ChainSolver.Params>): Model {
        val geometries = model.minecraftGeometry() ?: return model
        val geo = geometries.firstOrNull() ?: return model
        val props = geo.modelProperties() ?: return model
        val scaleU = alpha.width / props.textureWidth()
        val scaleV = alpha.height / props.textureHeight()

        val out = ArrayList<Bone>(geo.bones().size)
        for (bone in geo.bones()) {
            val name = bone.name()
            val params = if (name == null) null else tables.entries.firstOrNull { matches(it.key, name) }?.value
            val links = if (name == null || params == null) emptyList() else linksFor(bone, params, alpha, scaleU, scaleV)
            if (links.isEmpty()) {
                out += bone
                continue
            }
            out += withoutCubes(anchorTop(bone, alpha, scaleU, scaleV))
            out += links
        }
        val replaced = Array(geometries.size) { i -> if (i == 0) MinecraftGeometry(out.toTypedArray(), geo.cape(), props) else geometries[i] }
        return Model(model.formatVersion(), replaced)
    }

    //moving the card's pivot to the top of its opaque span, where its chains actually hang from
    private fun anchorTop(bone: Bone, alpha: Alpha, scaleU: Double, scaleV: Double): Bone {
        val cube = (bone.cubes() ?: arrayOf()).firstOrNull() ?: return bone
        val org = cube.origin() ?: return bone
        val size = cube.size() ?: return bone
        val face = cube.uv()?.faceUV()?.north()
        val uv = face?.uv() ?: return bone
        val cols = face.uvSize()[0].roundToInt()
        val rows = face.uvSize()[1].roundToInt()
        var lo = -1
        var hi = -1
        for (c in 0 until cols) {
            if (anyOpaque(alpha, uv, c, 0, rows, scaleU, scaleV)) {
                if (lo < 0) lo = c
                hi = c
            }
        }
        if (lo < 0) return bone
        val xPer = size[0] / cols
        val top = doubleArrayOf(org[0] + (lo + hi + 1) / 2.0 * xPer, org[1] + size[1], org[2])
        return Bone(bone.bindPoseRotation(), bone.cubes(), bone.debug(), bone.inflate(), bone.locators(), bone.mirror(), bone.name(), bone.neverRender(), bone.parent(), top, bone.polyMesh(), bone.renderGroupId(), bone.reset(), bone.rotation(), bone.textureMeshes())
    }

    private fun linksFor(bone: Bone, params: ChainSolver.Params, alpha: Alpha, scaleU: Double, scaleV: Double): List<Bone> {
        val links = ArrayList<Bone>()
        val span = params.step.coerceAtLeast(1)
        val columns = params.columns.coerceAtLeast(1)
        for ((ci, cube) in (bone.cubes() ?: arrayOf()).withIndex()) {
            val face = cube.uv()?.faceUV()?.north() ?: continue
            val org = cube.origin() ?: continue
            val size = cube.size() ?: continue
            val uv = face.uv() ?: continue
            val cols = face.uvSize()[0].roundToInt()
            val rows = face.uvSize()[1].roundToInt()
            if (cols <= 0 || rows <= 0) continue

            val xPer = size[0] / cols
            val yPer = size[1] / rows
            val cubePivot = cube.pivot() ?: bone.pivot()
            val cubeRot = cube.rotation() ?: doubleArrayOf(0.0, 0.0, 0.0)
            val chainRoot = bone.name()

            for (c in 0 until cols step columns) {
                val cw = (cols - c).coerceAtMost(columns)
                var parent = chainRoot
                var chain = 0
                var r = 0
                while (r < rows) {
                    val h = (rows - r).coerceAtMost(span)
                    if (!anyOpaqueSpan(alpha, uv, c, cw, r, h, scaleU, scaleV)) {
                        // A gap ends this chain; anything below it is a separate one
                        if (parent != chainRoot) chain++
                        parent = chainRoot
                        r += h
                        continue
                    }
                    val topY = org[1] + size[1] - r * yPer
                    val name = "${bone.name()}$LINK_MARK${ci}_${c}c${chain}_$r"
                    val top = doubleArrayOf(org[0] + (c + cw * 0.5) * xPer, topY, org[2])
                    val linkCube = Cube(
                        cube.inflate(), cube.mirror(),
                        doubleArrayOf(org[0] + c * xPer, topY - h * yPer, org[2]),
                        cubePivot, cubeRot,
                        doubleArrayOf(cw * xPer, h * yPer, size[2]),
                        UVUnion(DoubleArray(0), UVFaces(FaceUV(face.materialInstance(), doubleArrayOf(uv[0] + c, uv[1] + r), doubleArrayOf(cw.toDouble(), h.toDouble()), face.uvRotation()), null, null, null, null, null), false)
                    )
                    links += link(name, parent, bone, rotateAbout(top, cubePivot, cubeRot), linkCube)
                    parent = name
                    r += h
                }
            }
        }
        return links
    }

    private fun anyOpaqueSpan(alpha: Alpha, uv: DoubleArray, c: Int, cw: Int, r: Int, h: Int, scaleU: Double, scaleV: Double): Boolean {
        for (k in 0 until cw) if (anyOpaque(alpha, uv, c + k, r, h, scaleU, scaleV)) return true
        return false
    }
    private fun anyOpaque(alpha: Alpha, uv: DoubleArray, c: Int, r: Int, h: Int, scaleU: Double, scaleV: Double): Boolean {
        val x0 = ((uv[0] + c) * scaleU).toInt()
        val y0 = ((uv[1] + r) * scaleV).toInt()
        val x1 = (((uv[0] + c + 1) * scaleU).toInt()).coerceAtLeast(x0 + 1)
        val y1 = (((uv[1] + r + h) * scaleV).toInt()).coerceAtLeast(y0 + 1)
        for (y in y0 until y1) for (x in x0 until x1) {
            if (alpha.at(x, y) > 0) return true
        }
        return false
    }

    private fun rotateAbout(p: DoubleArray, pivot: DoubleArray, rot: DoubleArray): DoubleArray {
        if (rot[0] == 0.0 && rot[1] == 0.0 && rot[2] == 0.0) return p
        var x = p[0] - pivot[0]; var y = p[1] - pivot[1]; var z = p[2] - pivot[2]
        var c = cos(Math.toRadians(rot[0])); var s = sin(Math.toRadians(rot[0]))
        val ry = y * c - z * s; z = y * s + z * c; y = ry
        c = cos(Math.toRadians(rot[1])); s = sin(Math.toRadians(rot[1]))
        val rx = x * c + z * s; z = -x * s + z * c; x = rx
        c = cos(Math.toRadians(rot[2])); s = sin(Math.toRadians(rot[2]))
        val fx = x * c - y * s; y = x * s + y * c; x = fx
        return doubleArrayOf(x + pivot[0], y + pivot[1], z + pivot[2])
    }

    // NOTE: bone is a 15-component java record so these are positional
    private fun link(name: String, parent: String?, src: Bone, pivot: DoubleArray, cube: Cube) =
        Bone(null, arrayOf(cube), null, src.inflate(), null, src.mirror(), name, null, parent, pivot, null, null, null, doubleArrayOf(0.0, 0.0, 0.0), null)
    private fun withoutCubes(b: Bone) =
        Bone(b.bindPoseRotation(), arrayOf(), b.debug(), b.inflate(), b.locators(), b.mirror(), b.name(), b.neverRender(), b.parent(), b.pivot(), b.polyMesh(), b.renderGroupId(), b.reset(), b.rotation(), b.textureMeshes())

    // Case-insensitive glob with '*' wildcards, like "Pom*"
    fun matches(glob: String, name: String): Boolean {
        if (!glob.contains('*')) return glob.equals(name, ignoreCase = true)
        val parts = glob.split('*')
        var at = 0
        for ((i, part) in parts.withIndex()) {
            if (part.isEmpty()) continue
            val found = name.indexOf(part, at, ignoreCase = true)
            if (found < 0) return false
            if (i == 0 && found != 0) return false
            at = found + part.length
        }
        return parts.last().isEmpty() || name.endsWith(parts.last(), ignoreCase = true)
    }

    private class Alpha(private val a: ByteArray, val width: Int, val height: Int) {
        fun at(x: Int, y: Int): Int = if (x < 0 || y < 0 || x >= width || y >= height) 0 else a[y * width + x].toInt() and 0xFF

        companion object {
            fun read(bytes: ByteArray): Alpha? = runCatching {
                NativeImage.read(java.io.ByteArrayInputStream(bytes)).use { img ->
                    val out = ByteArray(img.width * img.height)
                    for (y in 0 until img.height) for (x in 0 until img.width) {
                        out[y * img.width + x] = (img.getPixelRGBA(x, y) ushr 24).toByte()
                    }
                    Alpha(out, img.width, img.height)
                }
            }.getOrNull()
        }
    }
}