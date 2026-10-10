package com.flooferland.showbiz.types.physics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.joml.*
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.cache.`object`.GeoCube
import kotlin.math.atan2

/**
 * One of Faz-Anim's DynamicBone chains, rebuilt from how DynamicBone is documented to behave.
 * The root is pinned where the animation puts it, a free particle at its end is pulled back toward rest, and the root turns to point at it
 */
class DynBone(@JvmField val root: String, @JvmField val params: Params) {
    @JvmField val boneNames: List<String> = listOf(root) + params.links

    private val pos: Vector3f = Vector3f()
    private val prev: Vector3f = Vector3f()
    private var ready: Boolean = false
    private val p0: Vector3f = Vector3f()
    private val rest: Vector3f = Vector3f()
    private val axis: Vector3f = Vector3f()
    private var end: Vector3f? = null

    private var dev: Float = 0f
    private var lastDev: Float = 0f
    private val turn: Quaternionf = Quaternionf()
    private val lastTurn: Quaternionf = Quaternionf()

    private val m: Matrix4f = Matrix4f()
    private val d: Vector3f = Vector3f()
    private val u: Vector3f = Vector3f()
    private val w: Vector3f = Vector3f()
    private val c: Vector3f = Vector3f()
    private val q: Quaternionf = Quaternionf()
    private val r: Quaternionf = Quaternionf()

    fun step(bones: (String) -> GeoBone?, delta: Float) {
        if (params.links.isNotEmpty()) return stepLinked(bones, delta)
        val bone = bones(root) ?: return
        animated(bone)
        if (!ready) {
            pos.set(rest); prev.set(rest)
            ready = true
        }
        val timeVar = delta * params.updateRate
        val restLen = rest.distance(p0)
        if (restLen < 1e-6f) return
        val vx = pos.x - prev.x; val vy = pos.y - prev.y; val vz = pos.z - prev.z
        prev.set(pos)
        pos.add(vx * (1f - params.damping), vy * (1f - params.damping), vz * (1f - params.damping))
        d.set(rest).sub(pos)
        pos.fma(params.elasticity * timeVar, d)
        if (params.stiffness > 0f) {
            d.set(rest).sub(pos)
            val len = d.length()
            val maxLen = restLen * (1f - params.stiffness) * 2f
            if (len > maxLen) pos.fma((len - maxLen) / len, d)
        }
        if (params.freezeX) {
            d.set(pos).sub(p0)
            pos.fma(-d.dot(axis), axis)
        }
        d.set(p0).sub(pos)
        val len = d.length()
        if (len > 1e-6f) pos.fma((len - restLen) / len, d)
        lastDev = dev
        lastTurn.set(turn)
        if (params.freezeX) {
            u.set(rest).sub(p0); w.set(pos).sub(p0)
            dev = atan2(u.cross(w, c).dot(axis), u.dot(w))
        } else {
            // The turn in model space, carried into the root's own frame so it rides on whatever the animation does next
            model(bone).getNormalizedRotation(r)
            q.rotationTo(u.set(rest).sub(p0).normalize(), w.set(pos).sub(p0).normalize())
            Quaternionf(r).conjugate().mul(q).mul(r, turn)
        }
    }

    // Adding the flop onto the bones, [alpha] of the way from the step before the last to the last
    fun draw(bones: (String) -> GeoBone?, alpha: Float) {
        if (params.links.isNotEmpty()) {
            for ((k, name) in boneNames.withIndex()) {
                if (k >= turns.size) return
                turnBone(bones(name) ?: return, q.set(lastTurns[k]).slerp(turns[k], alpha))
            }
            return
        }
        val bone = bones(root) ?: return
        if (params.freezeX) bone.rotX += lastDev + (dev - lastDev) * alpha
        else turnBone(bone, q.set(lastTurn).slerp(turn, alpha))
    }

    // A chain of links: one particle per bone's pivot, root first, then the last bone's end
    private var parts: Array<Vector3f> = emptyArray()
    private var prevs: Array<Vector3f> = emptyArray()
    private var anims: Array<Vector3f> = emptyArray()

    private var turns: Array<Quaternionf> = emptyArray()
    private var lastTurns: Array<Quaternionf> = emptyArray()

    private fun stepLinked(bones: (String) -> GeoBone?, delta: Float) {
        val chain = boneNames.map { bones(it) ?: return }
        val size = chain.size
        val last = chain.last()
        val end = this.end ?: measure(last).also { this.end = it }
        if (!ready) {
            parts = Array(size + 1) { Vector3f() }; prevs = Array(size + 1) { Vector3f() }; anims = Array(size + 1) { Vector3f() }
            turns = Array(size) { Quaternionf() }; lastTurns = Array(size) { Quaternionf() }
        }
        for (i in 0 until size) model(chain[i]).transformPosition(anims[i].set(chain[i].pivotX / 16f, chain[i].pivotY / 16f, chain[i].pivotZ / 16f))
        model(last).transformPosition(anims[size].set(last.pivotX / 16f + end.x, last.pivotY / 16f + end.y, last.pivotZ / 16f + end.z))
        if (!ready) {
            for (i in 0..size) { parts[i].set(anims[i]); prevs[i].set(anims[i]) }
            ready = true
        }
        val timeVar = delta * params.updateRate
        prevs[0].set(parts[0]); parts[0].set(anims[0])
        for (i in 1..size) {
            val vx = parts[i].x - prevs[i].x; val vy = parts[i].y - prevs[i].y; val vz = parts[i].z - prevs[i].z
            prevs[i].set(parts[i])
            parts[i].add(vx * (1f - params.damping), vy * (1f - params.damping), vz * (1f - params.damping))
        }
        for (i in 1..size) {
            val p = parts[i]; val up = parts[i - 1]
            val restLen = anims[i].distance(anims[i - 1])
            c.set(anims[i]).sub(anims[i - 1]).add(up)
            d.set(c).sub(p)
            p.fma(params.elasticity * timeVar, d)
            if (params.stiffness > 0f) {
                d.set(c).sub(p)
                val len = d.length()
                val maxLen = restLen * (1f - params.stiffness) * 2f
                if (len > maxLen) p.fma((len - maxLen) / len, d)
            }
            if (params.freezeX) {
                model(chain[i - 1]).transformDirection(axis.set(1f, 0f, 0f)).normalize()
                d.set(p).sub(up)
                p.fma(-d.dot(axis), axis)
            }
            d.set(up).sub(p)
            val len = d.length()
            if (len > 1e-6f) p.fma((len - restLen) / len, d)
        }
        val saved = chain.map { floatArrayOf(it.rotX, it.rotY, it.rotZ) }
        for (i in 0 until size) {
            lastTurns[i].set(turns[i])
            val pivot = model(chain[i]).transformPosition(Vector3f(chain[i].pivotX / 16f, chain[i].pivotY / 16f, chain[i].pivotZ / 16f))
            val child = if (i + 1 < size) model(chain[i + 1]).transformPosition(Vector3f(chain[i + 1].pivotX / 16f, chain[i + 1].pivotY / 16f, chain[i + 1].pivotZ / 16f))
                else model(chain[i]).transformPosition(Vector3f(last.pivotX / 16f + end.x, last.pivotY / 16f + end.y, last.pivotZ / 16f + end.z))
            q.rotationTo(u.set(child).sub(pivot).normalize(), w.set(parts[i + 1]).sub(parts[i]).normalize())
            model(chain[i]).getNormalizedRotation(r)
            Quaternionf(r).conjugate().mul(q).mul(r, turns[i])
            turnBone(chain[i], turns[i])
        }
        for ((k, b) in chain.withIndex()) { b.rotX = saved[k][0]; b.rotY = saved[k][1]; b.rotZ = saved[k][2] }
    }

    /** The bone turned by [by], back into GeckoLib Z-Y-X angles */
    private fun turnBone(bone: GeoBone, by: Quaternionf) {
        val vec = Quaterniond().rotateZ(bone.rotZ.toDouble()).rotateY(bone.rotY.toDouble()).rotateX(bone.rotX.toDouble())
            .mul(Quaterniond(by.x.toDouble(), by.y.toDouble(), by.z.toDouble(), by.w.toDouble()))
            .getEulerAnglesZYX(Vector3d())
        bone.rotX = vec.x.toFloat(); bone.rotY = vec.y.toFloat(); bone.rotZ = vec.z.toFloat()
    }

    /** The end, like Faz-Anim's _end bones: from the pivot along the bone's cubes, [Params.length] or as far as they reach.
     *  Held to its X plane it is squared to X; swinging free it keeps the cubes' own direction */
    private fun measure(bone: GeoBone): Vector3f {
        val cubes = bone.cubes.ifEmpty { cubesBelow(bone) }
        val sum = Vector3f()
        var n = 0
        for (cube in cubes) {
            for (quad in cube.quads().filterNotNull()) {
                for (v in quad.vertices()) {
                    sum.add(v.position())
                    n++
                }
            }
        }
        val pivot = Vector3f(bone.pivotX, bone.pivotY, bone.pivotZ).div(16f)
        val dir = sum.div(maxOf(n, 1).toFloat()).sub(pivot).let { if (params.freezeX) it.setComponent(0, 0f) else it }.normalize()
        var reach = 0f
        for (cube in cubes) {
            for (quad in cube.quads().filterNotNull()) {
                for (vertex in quad.vertices())
                    reach = maxOf(reach, Vector3f(vertex.position()).sub(pivot).dot(dir))
            }
        }
        return dir.mul(if (params.length > 0f) params.length / 16f else reach)
    }

    private fun cubesBelow(bone: GeoBone): List<GeoCube> = bone.childBones.flatMap { it.cubes + cubesBelow(it) }

    // Where the animation puts the root and its end this step, in model space
    private fun animated(bone: GeoBone) {
        val end = this.end ?: measure(bone).also { this.end = it }
        model(bone)
        m.transformPosition(p0.set(bone.pivotX / 16f, bone.pivotY / 16f, bone.pivotZ / 16f))
        m.transformPosition(rest.set(bone.pivotX / 16f + end.x, bone.pivotY / 16f + end.y, bone.pivotZ / 16f + end.z))
        m.transformDirection(axis.set(1f, 0f, 0f)).normalize()
    }

    // A bone's model-space transform into [m], built the way GeckoLib's RenderUtil.prepMatrixForBone builds it, root first
    private fun model(bone: GeoBone): Matrix4f {
        val parent = bone.parent
        if (parent != null) model(parent) else m.identity()
        m.translate(-bone.posX / 16f, bone.posY / 16f, bone.posZ / 16f)
        m.translate(bone.pivotX / 16f, bone.pivotY / 16f, bone.pivotZ / 16f)
        m.rotateZ(bone.rotZ).rotateY(bone.rotY).rotateX(bone.rotX)
        m.scale(bone.scaleX, bone.scaleY, bone.scaleZ)
        m.translate(-bone.pivotX / 16f, -bone.pivotY / 16f, -bone.pivotZ / 16f)
        return m
    }

    // One [dynbones."<bone>"] table of a bot's physics.toml, with Faz-Anim's DynamicBone settings for the chain rooted at that bone
    @Serializable
    data class Params(
        val damping: Float,                                   // speed lost each step
        val elasticity: Float,                                // pull back toward where the animation puts the end
        val stiffness: Float = 0f,                            // how close to it the end is held, 0 is free
        @SerialName("update_rate") val updateRate: Int = 60,  // scales how hard the part gets pulled back each frame (elasticity × rate / 60)
        @SerialName("freeze_x") val freezeX: Boolean,         // swings only in the plane across the root's X axis
        val length: Float = 0f,                               // where the end sits, in pixels, 0 is as far as the bone's cubes reach
        val links: List<String> = listOf()                    // the bones below the root, for a chain of more than one
    )
}