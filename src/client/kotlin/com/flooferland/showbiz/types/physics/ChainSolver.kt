package com.flooferland.showbiz.types.physics

import com.flooferland.showbiz.addons.data.ChainLayout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.joml.Matrix3f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.*

class ChainSolver(@JvmField val params: Params, @JvmField val chains: Array<ChainState>, @JvmField val members: IntArray) {
    fun interface Lengths { fun of(chain: Int): Float }
    fun interface LinkPose { fun of(chain: Int, link: Int): Matrix4fc? }

    // Each chain's root transform this frame; null skips the chain
    @JvmField val roots: Array<Matrix4fc?> = arrayOfNulls(chains.size)

    private val m3: Matrix3f = Matrix3f()
    private val v: Vector3f = Vector3f()
    private val va: Vector3f = Vector3f()
    private val vb: Vector3f = Vector3f()
    private val qTot: Quaternionf = Quaternionf()
    private val qLocal: Quaternionf = Quaternionf()
    private val prevQ: Quaternionf = Quaternionf()
    private val eul: Vector3f = Vector3f()
    private val renderConeCos: Float = cos(Math.toRadians(params.renderConeDeg.toDouble())).toFloat()
    private val pinRate: Float = -ln(1f - (params.anchorSmooth * REF).coerceAtMost(0.95f)) / REF

    // The pin this chain follows this frame, after smoothing and lag
    private var rvx: Float = 0f
    private var rvy: Float = 0f
    private var rvz: Float = 0f

    init {
        for (s in chains) {
            val perJoint = (params.maxBendDeg / s.segs).coerceAtLeast(1f)
            s.cosMax = cos(Math.toRadians(perJoint.toDouble())).toFloat()
            s.cosSoft = cos(Math.toRadians((perJoint * (1f - params.softBand)).toDouble())).toFloat()
        }
    }

    fun frame(delta: Float, lengths: Lengths, linkPose: LinkPose) {
        // One push a frame would be a push rate that rises with the frame rate
        if (params.repel > 0f && chains.size > 1) repel((delta / REF).coerceAtMost(3f))
        for (i in chains.indices) {
            val s = chains[i]
            s.posed = false
            val root = roots[i] ?: continue
            if (advance(s, root, delta, lengths, i)) pose(s, root, linkPose, i)
        }
    }

    // Fake collision

    // Joints of different chains at the same depth push apart, joint and prev together so no velocity is invented. Free at the tip, held at the knot
    private fun repel(rate: Float) {
        val first = chains[0]
        val rad = first.totalLen * params.repelRadius
        val r2 = rad * rad
        val maxPush = if (params.repelCap > 0f) first.totalLen * params.repelCap else Float.MAX_VALUE
        var depth = Int.MAX_VALUE
        for (s in chains) if (s.n < depth) depth = s.n
        for (k in 1 until depth) repelDepth(k, k.toFloat() / (depth - 1), rad, r2, maxPush, rate)
    }
    private fun repelDepth(k: Int, f: Float, rad: Float, r2: Float, maxPush: Float, rate: Float) {
        val o = k * 3
        for (i in chains.indices) {
            val si = chains[i]
            if (!si.ready) continue
            for (j in i + 1 until chains.size) {
                val sj = chains[j]
                if (!sj.ready) continue
                val a = si.pos; val b = sj.pos
                val dx = b[o] - a[o]; val dy = b[o + 1] - a[o + 1]; val dz = b[o + 2] - a[o + 2]
                val d2 = dx * dx + dy * dy + dz * dz
                if (d2 >= r2 || d2 < 1e-12f) continue
                val d = sqrt(d2)
                val push = ((rad - d) * 0.5f * params.repel * f * rate).coerceAtMost(maxPush)
                val px = dx / d * push; val py = dy / d * params.repelUp * push; val pz = dz / d * push
                a[o] = a[o] - px; a[o + 1] = a[o + 1] - py; a[o + 2] = a[o + 2] - pz
                b[o] = b[o] + px; b[o + 1] = b[o + 1] + py; b[o + 2] = b[o + 2] + pz
                val ap = si.prev; val bp = sj.prev
                ap[o] = ap[o] - px; ap[o + 1] = ap[o + 1] - py; ap[o + 2] = ap[o + 2] - pz
                bp[o] = bp[o] + px; bp[o + 1] = bp[o + 1] + py; bp[o + 2] = bp[o + 2] + pz
            }
        }
    }

    // One chain, one frame

    // True when the chain simulated and should be posed this frame
    private fun advance(s: ChainState, root: Matrix4fc, delta: Float, lengths: Lengths, i: Int): Boolean {
        rvx = root.m30(); rvy = root.m31(); rvz = root.m32()
        if (s.ready && params.anchorSmooth > 0f) followPin(s, delta)
        if (!s.ready) {
            build(s, lengths.of(i))
            restart(s)
            return false
        }
        s.rootVelX = rvx - s.rootPrevX; s.rootVelY = rvy - s.rootPrevY; s.rootVelZ = rvz - s.rootPrevZ
        // A jump longer than the chain is a hitch, not motion: carrying the chain and dropping its velocity
        if (len(s.rootVelX, s.rootVelY, s.rootVelZ) > s.totalLen.coerceAtLeast(0.05f)) {
            resume(s)
            restart(s)
            return false
        }
        if (sleep(s, delta)) return false
        delayPin(s, delta)
        val h = params.fixedStep
        val before = s.acc
        s.acc += delta
        var steps = (s.acc / h).toInt()
        if (steps > params.subStepCap) {
            steps = params.subStepCap
            s.acc = 0f
        } else {
            s.acc -= steps * h
        }
        val dampStep = params.damp.coerceIn(0.3f, 0.999f).toDouble().pow(h.toDouble()).toFloat()
        val scale = h / REF
        for (step in 1..steps) {
            s.pos.copyInto(s.drawPrev)
            // Where this step falls inside the frame, for the hand's position at that instant
            val t = ((step * h - before) / delta).coerceIn(0f, 1f)
            fixedStep(s, root, t, h, dampStep, scale)
        }
        if (steps > 0 && stepSpeed(s, len(s.rootVelX, s.rootVelY, s.rootVelZ) / delta) < params.sleepEps) s.pos.copyInto(s.prev)
        val a = (s.acc / h).coerceIn(0f, 1f)
        for (o in 0 until s.n * 3) s.draw[o] = s.drawPrev[o] + (s.pos[o] - s.drawPrev[o]) * a
        s.rootPrevX = rvx; s.rootPrevY = rvy; s.rootPrevZ = rvz
        return true
    }

    // First frame: hanging the chain straight down from the pin
    private fun build(state: ChainState, total: Float) {
        state.totalLen = total
        state.seg = total / state.segs
        var y = rvy
        for (i in 0 until state.n) {
            val o = i * 3
            state.pos[o] = rvx; state.pos[o + 1] = y; state.pos[o + 2] = rvz
            state.prev[o] = rvx; state.prev[o + 1] = y; state.prev[o + 2] = rvz
            if (i < state.segs) y -= state.seg
        }
        state.rootPrevX = rvx; state.rootPrevY = rvy; state.rootPrevZ = rvz
        state.pinX = rvx; state.pinY = rvy; state.pinZ = rvz
        state.ready = true
    }
    private fun resume(state: ChainState) {
        val dx = state.rootVelX; val dy = state.rootVelY; val dz = state.rootVelZ
        for (o in 0 until state.n * 3 step 3) {
            state.pos[o] = state.pos[o] + dx; state.pos[o + 1] = state.pos[o + 1] + dy; state.pos[o + 2] = state.pos[o + 2] + dz
        }
        state.pos.copyInto(state.prev)
        state.pinX = rvx; state.pinY = rvy; state.pinZ = rvz
        state.rootPrevX = rvx; state.rootPrevY = rvy; state.rootPrevZ = rvz
        state.rootVelX = 0f; state.rootVelY = 0f; state.rootVelZ = 0f
    }
    private fun restart(state: ChainState) {
        state.acc = 0f
        state.pos.copyInto(state.drawPrev)
        state.pos.copyInto(state.draw)
    }

    private fun followPin(state: ChainState, delta: Float) {
        val a = 1f - exp(-pinRate * delta)
        state.pinX += (rvx - state.pinX) * a
        state.pinY += (rvy - state.pinY) * a
        state.pinZ += (rvz - state.pinZ) * a
        val ex = rvx - state.pinX; val ey = rvy - state.pinY; val ez = rvz - state.pinZ
        val e = sqrt((ex * ex + ey * ey + ez * ez).toDouble()).toFloat()
        if (e > params.pinLagMax && e > 1e-6f) {
            val pull = (e - params.pinLagMax) / e
            state.pinX += ex * pull; state.pinY += ey * pull; state.pinZ += ez * pull
        }
        rvx = state.pinX; rvy = state.pinY; rvz = state.pinZ
    }

    // The fastest joint: a step's displacement over one 60 fps frame
    private fun stepSpeed(state: ChainState, floor: Float): Float {
        var top = floor
        for (o in 0 until state.n * 3 step 3) {
            val vx = state.pos[o] - state.prev[o]
            val vy = state.pos[o + 1] - state.prev[o + 1]
            val vz = state.pos[o + 2] - state.prev[o + 2]
            val sp = sqrt(vx * vx + vy * vy + vz * vz) / REF
            if (sp > top) top = sp
        }
        return top
    }

    // Asleep once the hand AND the chain have held still for a tick (the hand alone froze chains mid-swing)
    private fun sleep(state: ChainState, delta: Float): Boolean {
        state.anchorSpeed = len(state.rootVelX, state.rootVelY, state.rootVelZ) / delta
        if (state.anchorSpeed < params.wakeEps && stepSpeed(state, 0f) < params.sleepEps) {
            state.stillTime += delta
            if (state.stillTime >= 3f * REF) {
                if (!state.asleep) {
                    state.pos.copyInto(state.prev)
                    state.asleep = true
                }
                state.rootPrevX = rvx; state.rootPrevY = rvy; state.rootPrevZ = rvz
                return true
            }
        } else {
            state.stillTime = 0f
            state.asleep = false
        }
        return false
    }

    // Each chain trails the pin by 0..lag 60 fps frames of time, picked by its name, so the bundle is not one object
    private fun delayPin(state: ChainState, delta: Float) {
        if (params.lag <= 0) return
        state.lagFrames = abs(state.lagHash / 7) % (params.lag + 1)
        state.clock += delta
        val slot = state.lagN % ChainState.LAG_SLOTS
        state.lagT[slot] = state.clock
        state.lagP[slot * 3] = rvx; state.lagP[slot * 3 + 1] = rvy; state.lagP[slot * 3 + 2] = rvz
        state.lagN++
        if (state.lagFrames <= 0) return
        pinAt(state, state.clock - state.lagFrames * REF)
    }
    private fun pinAt(state: ChainState, want: Float) {
        val count = state.lagN.coerceAtMost(ChainState.LAG_SLOTS)
        var newer = (state.lagN - 1) % ChainState.LAG_SLOTS
        for (back in 1 until count) {
            val older = (state.lagN - 1 - back) % ChainState.LAG_SLOTS
            if (state.lagT[older] <= want) {
                val span = state.lagT[newer] - state.lagT[older]
                val f = if (span > 1e-9f) ((want - state.lagT[older]) / span).coerceIn(0f, 1f) else 1f
                rvx = state.lagP[older * 3] + (state.lagP[newer * 3] - state.lagP[older * 3]) * f
                rvy = state.lagP[older * 3 + 1] + (state.lagP[newer * 3 + 1] - state.lagP[older * 3 + 1]) * f
                rvz = state.lagP[older * 3 + 2] + (state.lagP[newer * 3 + 2] - state.lagP[older * 3 + 2]) * f
                return
            }
            newer = older
        }
        rvx = state.lagP[newer * 3]; rvy = state.lagP[newer * 3 + 1]; rvz = state.lagP[newer * 3 + 2]
    }

    //one step

    private fun fixedStep(state: ChainState, root: Matrix4fc, t: Float, h: Float, dampStep: Float, scale: Float) {
        state.pos[0] = state.rootPrevX + (rvx - state.rootPrevX) * t
        state.pos[1] = state.rootPrevY + (rvy - state.rootPrevY) * t
        state.pos[2] = state.rootPrevZ + (rvz - state.rootPrevZ) * t
        state.prev[0] = state.pos[0]; state.prev[1] = state.pos[1]; state.prev[2] = state.pos[2]
        verlet(state, dampStep.coerceIn(0.05f, 0.9999f), state.seg * 0.8f * h, 1f / h, params.gravity * h * h)
        constrain(state, scale)
        if (params.bendStiff > 0f) {
            bend(state, scale)
            relength(state)
        }
        dampAfter(state, dampStep)
        if (params.spring > 0f && state.restKnown) spring(state, root, h)
    }

    // one Verlet step. [cap] is the most a joint may move, [subRate] turns a step's displacement into blocks per tick
    private fun verlet(state: ChainState, dK: Float, cap: Float, subRate: Float, gStep: Float) {
        val pos = state.pos
        val prev = state.prev
        val rest2 = params.sleepEps * params.sleepEps
        for (o in 3 until state.n * 3 step 3) {
            val px = pos[o]; val py = pos[o + 1]; val pz = pos[o + 2]
            var vx = (px - prev[o]) * dK
            var vy = (py - prev[o + 1]) * dK
            var vz = (pz - prev[o + 2]) * dK
            val sp = sqrt(vx * vx + vy * vy + vz * vz)
            if (sp > cap) { val f = cap / sp; vx *= f; vy *= f; vz *= f }
            if ((vx * vx + vy * vy + vz * vz) * subRate * subRate < rest2) { vx = 0f; vy = 0f; vz = 0f }
            prev[o] = px; prev[o + 1] = py; prev[o + 2] = pz
            pos[o] = px + vx; pos[o + 1] = py + vy - gStep; pos[o + 2] = pz + vz
        }
    }

    private fun constrain(state: ChainState, stepScale: Float) {
        val sweeps = state.n.coerceAtMost(20)
        for (sw in 1..sweeps) {
            if (!sweep(state, stepScale) && params.earlyExit) return
        }
    }

    private fun sweep(state: ChainState, stepScale: Float): Boolean {
        val pos = state.pos
        val seg = state.seg
        val lim = seg * params.projClamp * stepScale
        val cosMax = state.cosMax
        val cosSoft = state.cosSoft
        var active = false
        for (k in 1 until state.n) {
            val a = (k - 1) * 3; val b = k * 3
            var dx = pos[b] - pos[a]; var dy = pos[b + 1] - pos[a + 1]; var dz = pos[b + 2] - pos[a + 2]
            var len = sqrt(dx * dx + dy * dy + dz * dz)
            if (len < 1e-6f) continue
            var ux = 0f; var uy = -1f; var uz = 0f
            if (k >= 2) {
                val c = (k - 2) * 3
                ux = pos[a] - pos[c]; uy = pos[a + 1] - pos[c + 1]; uz = pos[a + 2] - pos[c + 2]
                val ul = sqrt(ux * ux + uy * uy + uz * uz)
                if (ul < 1e-6f) { ux = 0f; uy = -1f; uz = 0f } else { ux /= ul; uy /= ul; uz /= ul }
            }
            val nx = dx / len; val ny = dy / len; val nz = dz / len
            val dot = (nx * ux + ny * uy + nz * uz).coerceIn(-1f, 1f)
            if (dot < cosSoft) {
                active = true
                val t = if (cosSoft > cosMax) ((cosSoft - dot) / (cosSoft - cosMax)).coerceIn(0f, 1f) else 1f
                val target = dot + (cosMax - dot) * t
                var bx = ux * target + (nx - ux * dot)
                var by = uy * target + (ny - uy * dot)
                var bz = uz * target + (nz - uz * dot)
                val bl = sqrt(bx * bx + by * by + bz * bz)
                if (bl > 1e-6f) { bx /= bl; by /= bl; bz /= bl; dx = bx; dy = by; dz = bz; len = 1f }
            }
            val f = seg / len
            var tx = pos[a] + dx * f; var ty = pos[a + 1] + dy * f; var tz = pos[a + 2] + dz * f

            // Capping each correction and letting the rest land next sweep: no jump at the tip
            val mx = tx - pos[b]; val my = ty - pos[b + 1]; val mz = tz - pos[b + 2]
            val mv = sqrt(mx * mx + my * my + mz * mz)
            if (mv > lim && mv > 1e-9f) {
                active = true
                val sc = lim / mv
                tx = pos[b] + mx * sc; ty = pos[b + 1] + my * sc; tz = pos[b + 2] + mz * sc
            }
            pos[b] = tx; pos[b + 1] = ty; pos[b + 2] = tz
        }
        return active
    }

    private fun bend(state: ChainState, stepScale: Float) {
        val pos = state.pos
        val kb = (params.bendStiff * stepScale).coerceIn(0f, 0.5f)
        for (k in 1 until state.n - 1) {
            val a = (k - 1) * 3; val b = k * 3; val c = (k + 1) * 3
            val mx = (pos[a] + pos[c]) * 0.5f - pos[b]
            val my = (pos[a + 1] + pos[c + 1]) * 0.5f - pos[b + 1]
            val mz = (pos[a + 2] + pos[c + 2]) * 0.5f - pos[b + 2]
            pos[b] = pos[b] + mx * kb; pos[b + 1] = pos[b + 1] + my * kb; pos[b + 2] = pos[b + 2] + mz * kb
        }
    }

    private fun relength(state: ChainState) {
        val pos = state.pos
        for (k in 1 until state.n) {
            val a = (k - 1) * 3; val b = k * 3
            val dx = pos[b] - pos[a]; val dy = pos[b + 1] - pos[a + 1]; val dz = pos[b + 2] - pos[a + 2]
            val len = sqrt(dx * dx + dy * dy + dz * dz)
            if (len < 1e-6f) continue
            val f = state.seg / len
            pos[b] = pos[a] + dx * f; pos[b + 1] = pos[a + 1] + dy * f; pos[b + 2] = pos[a + 2] + dz * f
        }
    }

    private fun dampAfter(state: ChainState, d: Float) {
        val pos = state.pos
        val prev = state.prev
        for (o in 3 until state.n * 3) prev[o] = pos[o] - (pos[o] - prev[o]) * d
    }

    private fun spring(state: ChainState, root: Matrix4fc, delta: Float) {
        m3.set(root)
        val wr = m3.transform(v.set(state.restDir[0], state.restDir[1], state.restDir[2]))
        if (wr.lengthSquared() <= 1e-9f) return
        wr.normalize()
        val kPull = (params.spring * delta).coerceIn(0f, 0.25f)
        val p = state.pos
        var acc = 0f
        for (k in 1 until state.n) {
            acc += state.seg
            val w = kPull * exp(-(k - 1).toFloat() * params.springFalloff)
            val tx = p[0] + wr.x * acc; val ty = p[1] + wr.y * acc; val tz = p[2] + wr.z * acc
            val o = k * 3
            p[o] = p[o] + (tx - p[o]) * w; p[o + 1] = p[o + 1] + (ty - p[o + 1]) * w; p[o + 2] = p[o + 2] + (tz - p[o + 2]) * w
        }
    }

    // Joints to bone rotations

    private fun pose(state: ChainState, root: Matrix4fc, linkPose: LinkPose, i: Int) {
        val rb = m3.set(root)
        if (abs(rb.determinant()) < 1e-6f) return
        rb.invert()
        val src = state.draw
        val ox = src[0]; val oy = src[1]; val oz = src[2]
        for (o in 0 until state.n * 3 step 3) {
            rb.transform(v.set(src[o] - ox, src[o + 1] - oy, src[o + 2] - oz))
            state.lp[o] = v.x; state.lp[o + 1] = v.y; state.lp[o + 2] = v.z
        }
        directions(state)
        prevQ.identity()
        val calibrating = !state.restKnown
        for (k in 0 until state.links) {
            state.eulerOk[k] = false
            val o = k * 3
            val d = v.set(state.dirs[o], state.dirs[o + 1], state.dirs[o + 2])
            if (!d.isFinite || d.lengthSquared() < 1e-12f) continue
            if (calibrating) calibrate(state, k, rb, d, linkPose, i) else aim(state, k, d)
        }
        state.restKnown = true
        state.posed = true
    }

    //Each link's direction along the rope interpolated between neighbouring segments rather than snapped to one
    private fun directions(state: ChainState) {
        val nl = state.links
        for (k in 0 until nl) {
            val t = ((k + 0.5f) / nl) * state.segs
            val si = t.toInt().coerceIn(0, state.segs - 1)
            val fr = (t - si).coerceIn(0f, 1f)
            val sj = (si + 1).coerceAtMost(state.segs - 1)
            segDir(state, si, va)
            segDir(state, sj, vb)
            val o = k * 3
            v.set(va.x + (vb.x - va.x) * fr, va.y + (vb.y - va.y) * fr, va.z + (vb.z - va.z) * fr)
            if (v.lengthSquared() > 1e-12f) v.normalize()
            state.dirs[o] = v.x; state.dirs[o + 1] = v.y; state.dirs[o + 2] = v.z
        }
    }
    private fun segDir(state: ChainState, si: Int, out: Vector3f) {
        val a = si * 3; val b = (si + 1) * 3
        out.set(state.lp[b], state.lp[b + 1], state.lp[b + 2]).sub(state.lp[a], state.lp[a + 1], state.lp[a + 2])
        if (out.lengthSquared() > 1e-12f) out.normalize()
    }

    // First posed frame: recording the rest directions as simulated and as drawn
    private fun calibrate(state: ChainState, k: Int, rb: Matrix3f, d: Vector3f, linkPose: LinkPose, i: Int) {
        val o = k * 3
        state.restDir[o] = d.x; state.restDir[o + 1] = d.y; state.restDir[o + 2] = d.z
        val a0 = linkPose.of(i, k)
        val b0 = if (k + 1 < state.links) linkPose.of(i, k + 1) else null
        if (a0 != null && b0 != null) {
            val rr = rb.transform(va.set(b0.m30() - a0.m30(), b0.m31() - a0.m31(), b0.m32() - a0.m32()))
            if (rr.lengthSquared() > 1e-10f) {
                rr.normalize()
                state.renderRest[o] = rr.x; state.renderRest[o + 1] = rr.y; state.renderRest[o + 2] = rr.z
            }
        }
        if (va.set(state.renderRest[o], state.renderRest[o + 1], state.renderRest[o + 2]).lengthSquared() < 1e-10f) {
            state.renderRest[o] = d.x; state.renderRest[o + 1] = d.y; state.renderRest[o + 2] = d.z
        }
    }

    private fun aim(state: ChainState, k: Int, d: Vector3f) {
        val o = k * 3
        val r0x = state.restDir[o]; val r0y = state.restDir[o + 1]; val r0z = state.restDir[o + 2]
        val cone = renderConeCos
        val dotR = (r0x * d.x + r0y * d.y + r0z * d.z).coerceIn(-1f, 1f)
        if (dotR < cone) {
            // Clamping to a cone about the rest direction: rotationTo is degenerate near antipodal
            val px = d.x - r0x * dotR; val py = d.y - r0y * dotR; val pz = d.z - r0z * dotR
            val pl = sqrt(px * px + py * py + pz * pz)
            if (pl > 1e-6f) {
                val sn = sqrt((1f - cone * cone).coerceAtLeast(0f))
                d.set(r0x * cone + px / pl * sn, r0y * cone + py / pl * sn, r0z * cone + pz / pl * sn).normalize()
            } else {
                d.set(r0x, r0y, r0z)
            }
        }
        qTot.rotationTo(va.set(state.renderRest[o], state.renderRest[o + 1], state.renderRest[o + 2]), d)
        qLocal.set(prevQ).conjugate().mul(qTot)
        qLocal.getEulerAnglesZYX(eul) // GeckoLib builds a bone as Rz * Ry * Rx (RenderUtil.rotateMatrixAroundBone), which is ZYX Euler order
        state.euler[o] = eul.x; state.euler[o + 1] = eul.y; state.euler[o + 2] = eul.z
        state.eulerOk[k] = eul.isFinite
        prevQ.set(qTot)
    }

    private fun len(x: Float, y: Float, z: Float): Float = va.set(x, y, z).length()

    // one [chains."pattern"] table of a bot's physics.toml: which bones are cut into chains, and how they move
    @Serializable
    data class Params(
        @SerialName("col_step") val columns: Int,               // how many pixels wide each chain is
        val step: Int,                                          // how many pixels tall each link is
        @SerialName("sim_segments") val segments: Int,          // physics points per chain (more is smoother, but slower)
        val gravity: Float,                                     // how hard chains fall (0.08 is Minecraft's gravity)
        @SerialName("rope_damp") val damp: Float,               // speed kept each tick: lower stops sooner, higher swings longer
        @SerialName("stiff") val spring: Float,                 // pull back to the modelled shape, 0 is a loose rope
        @SerialName("stiff_falloff") val springFalloff: Float,  // how fast that pull fades toward the tip
        @SerialName("bend_stiff") val bendStiff: Float,         // how much joints resist bending, smooths out kinks
        @SerialName("max_bend") val maxBendDeg: Float,          // bend limit in degrees (out of reach on purpose)
        @SerialName("soft_band") val softBand: Float,           // how gently that limit kicks in
        @SerialName("proj_clamp") val projClamp: Float,         // safety: the most a joint can jump in one step
        @SerialName("substep_cap") val subStepCap: Int,         // safety: the most physics steps in one frame
        val lag: Int,                                           // each chain trails the hand by up to this many frames
        @SerialName("anchor_smooth") val anchorSmooth: Float,   // smooths the hand: higher follows tighter, 0 is off
        @SerialName("pin_lag_max") val pinLagMax: Float,        // furthest the smoothing lets chains fall behind, in blocks
        @SerialName("sleep") val sleepEps: Float,               // slower than this, a chain rests and costs nothing...
        @SerialName("wake") val wakeEps: Float,                 // ...until the hand moves faster than this
        val repel: Float,                                       // how hard chains push each other apart, 0 is off
        @SerialName("repel_radius") val repelRadius: Float,     // how close chains get before pushing, in blocks
        @SerialName("repel_cap") val repelCap: Float,           // the most one push can move a joint, 0 is no limit
        @SerialName("repel_up") val repelUp: Float,             // how much pushes can go up or down, 0 is sideways only
        @SerialName("render_cone") val renderConeDeg: Float,    // most a link can turn from its modelled angle, in degrees
        @SerialName("fixed_step") val fixedStep: Float,         // physics step length in ticks, so it looks the same at any fps
        @SerialName("sweep_exit") val earlyExit: Boolean        // skips extra work once chains settle (off until checked)
    )

    companion object {
        const val REF: Float = 1f / 3f //One 60 fps frame in ticks: what every per-frame constant was tuned against
        fun of(params: Params, links: IntArray, lagHash: IntArray, members: IntArray = IntArray(links.size) { it }): ChainSolver =
            ChainSolver(params, Array(links.size) { i -> ChainState(params.segments.coerceIn(2, links[i].coerceAtLeast(2)), links[i], lagHash[i]) }, members)

        fun forTables(layout: ChainLayout, tables: Map<String, Params>): List<ChainSolver> {
            val byTable = LinkedHashMap<String, MutableList<Int>>()
            for (i in 0 until layout.size) {
                val pattern = tables.keys.firstOrNull { ChainRig.matches(it, layout.owners[i]) } ?: continue
                byTable.getOrPut(pattern) { ArrayList() } += i
            }
            return byTable.map { (pattern, list) ->
                val members = list.toIntArray()
                of(tables[pattern]!!, IntArray(members.size) { layout.linkCounts[members[it]] }, IntArray(members.size) { layout.lagHash[members[it]] }, members)
            }
        }
    }
}