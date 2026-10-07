package com.flooferland.showbiz.utils

import net.minecraft.core.*
import net.minecraft.core.component.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.server.level.*
import net.minecraft.util.*
import net.minecraft.world.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*
import java.util.Locale
import java.util.UUID
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import kotlin.math.roundToInt
import kotlin.reflect.KClass


@Suppress("unused")
object Extensions {
    fun ResourceLocation.blockPath(): ResourceLocation {
        return this.withPrefix("block/");
    }
    fun ResourceLocation.itemPath(): ResourceLocation {
        return this.withPrefix("item/");
    }
    fun <T> ItemStack.applyComponent(type: DataComponentType<T>, comp: T) {
        this.applyComponents(DataComponentPatch.builder().set(type, comp!!).build())
    }

    /** Calls setChanged and sendBlockUpdated */
    fun BlockEntity.markDirtyNotifyAll() {
        setChanged()
        level?.sendBlockUpdated(this.blockPos, blockState, blockState, Block.UPDATE_ALL)
    }

    fun Player.setInventoryChanged() {
        inventoryMenu.broadcastChanges()
        inventory.setChanged()
    }

    @DslMarker annotation class BlockEntityApplyDsl;

    fun <T: BlockEntity> T.applyChange(rerender: Boolean, change: T.() -> Unit) {
        change(this)
        markDirtyNotifyAll()
    }

    //region Level
    fun Level.getEntity(uuid: UUID) = (this as? ServerLevel)?.getEntity(uuid) ?: MainClientUtils.getEntityByUuid(this, uuid)
    fun Level.getNearbyPlayers(area: AABB) = this.players().filter { area.contains(it.position()) }
    //endregion

    //region GeckoLib
    fun GeoBone.getChildrenFlattened(): HashSet<GeoBone> {
        val bones = hashSetOf<GeoBone>(this)
        this.childBones.forEach { bones.addAll(it.getChildrenFlattened()) }
        return bones
    }
    fun BakedGeoModel.getAllBones(): HashSet<GeoBone> {
        val bones = hashSetOf<GeoBone>()
        topLevelBones.forEach {
            bones.add(it)
            bones.addAll(it.getChildrenFlattened())
        }
        return bones
    }
    //endregion

    // region VoxelShapes (thank you Skillet!!)
    /** Rotates a north shape to any direction */
    fun VoxelShape.rotateShape(to: Direction): VoxelShape {
        val buffer = arrayOf<VoxelShape>(this, Shapes.empty())
        val times: Int = (to.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4
        for (i in 0..<times) {
            buffer[0].forAllBoxes { minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double ->
                buffer[1] = Shapes.or(buffer[1], Shapes.box(1.0f - maxZ, minY, minX, 1.0f - minZ, maxY, maxX))
            }
            buffer[0] = buffer[1]
            buffer[1] = Shapes.empty()
        }
        return buffer[0]
    }
    fun VoxelShape.generateHorizontal() = hashMapOf(
        Direction.NORTH to rotateShape(Direction.NORTH).optimize(),
        Direction.SOUTH to rotateShape(Direction.SOUTH).optimize(),
        Direction.WEST to rotateShape(Direction.WEST).optimize(),
        Direction.EAST to rotateShape(Direction.EAST).optimize()
    )
    fun MutableMap<Direction, VoxelShape>.getCachedOrRotate(to: Direction): VoxelShape {
        return getOrPut(to) { (get(Direction.NORTH) ?: Shapes.block()).rotateShape(to).optimize() }
    }
    // endregion

    //region Compound functions, since these change for 1.21.5+
    fun CompoundTag.removeIfPresent(key: String)    = if (contains(key)) remove(key) else Unit
    fun CompoundTag.getOrNull(key: String)          = if (contains(key)) get(key) else null
    fun CompoundTag.getCompoundOrNull(key: String)  = if (contains(key)) getCompound(key) else null
    fun CompoundTag.getBooleanOrNull(key: String)   = if (contains(key)) getBoolean(key) else null
    fun CompoundTag.getByteOrNull(key: String)      = if (contains(key)) getByte(key) else null
    fun CompoundTag.getShortOrNull(key: String)     = if (contains(key)) getShort(key) else null
    fun CompoundTag.getIntOrNull(key: String)       = if (contains(key)) getInt(key) else null
    fun CompoundTag.getLongOrNull(key: String)      = if (contains(key)) getLong(key) else null
    fun CompoundTag.getFloatOrNull(key: String)     = if (contains(key)) getFloat(key) else null
    fun CompoundTag.getDoubleOrNull(key: String)    = if (contains(key)) getDouble(key) else null
    fun CompoundTag.getStringOrNull(key: String)    = if (contains(key)) getString(key) else null
    fun CompoundTag.getIntArrayOrNull(key: String)  = if (contains(key)) getIntArray(key) else null
    fun CompoundTag.getByteArrayOrNull(key: String) = if (contains(key)) getByteArray(key) else null
    fun CompoundTag.getLongArrayOrNull(key: String) = if (contains(key)) getLongArray(key) else null
    fun CompoundTag.getUUIDOrNull(key: String)      = if (contains(key)) getUUID(key) else null
    //endregion

    fun LivingEntity.getHeldItem(filter: (ItemStack) -> Boolean): ItemStack? =
        when {
            filter(mainHandItem) -> mainHandItem
            filter(offhandItem) -> offhandItem
            else -> null
        }
    fun Player.handItem(stack: ItemStack) = when {
        mainHandItem.isEmpty -> setItemInHand(InteractionHand.MAIN_HAND, stack)
        offhandItem.isEmpty -> setItemInHand(InteractionHand.OFF_HAND, stack)
        else -> inventory.add(stack)
    }

    //region Component
    fun MutableComponent.hover(text: String) = hover(Component.literal(text))
    fun MutableComponent.hover(comp: Component) =
        withStyle { it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, comp)) }!!
    fun MutableComponent.click(action: ClickEvent.Action, value: String) =
        withStyle { it.withClickEvent(ClickEvent(action, value)) }!!

    fun MutableComponent.withLink(url: String) = withStyle {
        it.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open this URL in your browser")))
            .withColor(FastColor.ARGB32.color(120, 200, 255))
            .withUnderlined(true)
            .withBold(true)
    }!!
    fun MutableComponent.withTeleport(pos: BlockPos) = withStyle {
        val coords = "${pos.x} ${pos.y} ${pos.z}"
        it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @s $coords"))
            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport to $coords")))
            .withColor(FastColor.ARGB32.color(120, 200, 255))
            .withUnderlined(true)
            .withBold(true)
    }!!

    fun MutableComponent.asLink() = withLink(string)
    fun BlockPos.toComp() = Component.literal("$x $y $z").withTeleport(this)
    //endregion

    fun KClass<*>.forceLoad() {
        sealedSubclasses.forEach { it.objectInstance ?: it.forceLoad() }
    }

    fun String.count(substring: String) = windowed(substring.length) { if (it == substring) 1 else 0 }.sum()
    fun String.alwaysEndsWith(suffix: String) = if (!endsWith(suffix)) this + suffix else this
    fun String.alwaysEndsWith(suffixes: Collection<String>): String {
        if (suffixes.any { this.endsWith(it) } || suffixes.isEmpty()) return this
        return this + suffixes.first()
    }

    fun Vec3.divide(factor: Double) = Vec3(x / factor, y / factor, z / factor)

    // region Number conversion funkiness
    fun Int.secsToTicks(): Int = this * 20
    fun Float.secsToTicks(): Int = (this * 20).roundToInt()
    fun Double.secsToTicks(): Int = (this * 20).roundToInt()

    fun Int.bytesToFriendlySize() = this.toLong().bytesToFriendlySize()
    fun Long.bytesToFriendlySize(): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            this >= gb -> String.format(Locale.US, "%.2f GB", this / gb)
            this >= mb -> String.format(Locale.US, "%.2f MB", this / mb)
            this >= kb -> String.format(Locale.US, "%.2f KB", this / kb)
            else -> "$this bytes"
        }
    }
    // endregion

    fun Number.formatDecimal(precision: Int = 3) =
        runCatching { "%.${precision}f".format(this) }.getOrDefault(toString())
}