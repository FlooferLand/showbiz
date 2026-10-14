package com.flooferland.showbiz.registry

import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.data.loot.*
import net.minecraft.data.models.*
import net.minecraft.data.models.blockstates.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.BlockBehaviour.*
import com.flooferland.showbiz.blocks.ChairBlock
import com.flooferland.showbiz.blocks.base.ShowbizDoorBlock
import com.flooferland.showbiz.items.base.FancyBlockItem
import com.flooferland.showbiz.utils.Extensions.blockPath
import com.flooferland.showbiz.utils.Extensions.forceLoad
import com.flooferland.showbiz.utils.rl

// NOTE: Could probably add the ability to pass in a datagen generator for making the model
//       Ex: { g -> g.createDoor(it) }

typealias BlockConstructor<B> = (BlockBehaviour.Properties) -> B

data class DecoEntry<B: Block>(
    val id: ResourceLocation,
    val block: B,
    val item: BlockItem,
    val transparent: Boolean,
    val parent: ModDecoBlocks
)

sealed class ModDecoBlocks {
    object RedDoor : BaseDoor("red_door")
    object VoronoiStripeCarpet : BaseCarpet("voronoi_stripe")
    object Chair : BaseChair("chair")


    // region | Base types
    sealed class BaseDoor(id: String) : ModDecoBlocks() {
        val entry = make<ShowbizDoorBlock>(
            id,
            transparent = true,
            constructor = ::ShowbizDoorBlock
        )
        override fun addLoot(p: BlockLootSubProvider) {
            p.add(entry.block, p.createDoorTable(entry.block))
        }
        override fun addBlockModels(g: BlockModelGenerators) {
            g.createDoor(entry.block)
        }
    }
    sealed class BaseChair(id: String) : ModDecoBlocks() {
        val entry = make<ChairBlock>(
            id,
            transparent = true,
            constructor = ::ChairBlock
        )
        override fun addLoot(p: BlockLootSubProvider) {
            p.add(entry.block, p.createSingleItemTable(entry.item))
        }
        override fun addBlockModels(g: BlockModelGenerators) {
            val blockModel = entry.id.blockPath()
            g.blockStateOutput.accept(
				MultiVariantGenerator.multiVariant(entry.block, Variant.variant().with(VariantProperties.MODEL, blockModel))
                    .with(BlockModelGenerators.createHorizontalFacingDispatch())
                    .with(BlockModelGenerators.createBooleanModelDispatch(ChairBlock.TUCKED, blockModel.withSuffix("_tucked"), blockModel))
			);
        }
    }
    sealed class BaseCarpet(name: String) : ModDecoBlocks() {
        val full = make<Block>(
            "${name}_block",
            constructor = { Block(it.sound(SoundType.WOOL)) }
        )
        val carpet = make<CarpetBlock>(
            "${name}_carpet",
            constructor = { CarpetBlock(it.sound(SoundType.WOOL)) }
        )
        override fun addBlockModels(g: BlockModelGenerators) {
            g.createFullAndCarpetBlocks(full.block, carpet.block)
        }
    }
    // endregion


    // region | ModDecoBlocks stuff
    val innerChildren = mutableSetOf<DecoEntry<*>>()
    open fun addLoot(p: BlockLootSubProvider) {
        for (child in innerChildren) {
            p.add(child.block, p.createSingleItemTable(child.item))
        }
    }
    abstract fun addBlockModels(g: BlockModelGenerators)
    open fun addItemModels(g: ItemModelGenerators) {}
    fun <B: Block> make(id: String, transparent: Boolean = false, constructor: BlockConstructor<B>): DecoEntry<B> {
        val id = rl(id)

        // Block
        val props = Properties.of()
            .strength(0.8f)
            .noOcclusion()
        val block = constructor(props)
        Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(BuiltInRegistries.BLOCK.key(), id), block)

        // Item
        val item = FancyBlockItem(id, block, Item.Properties())
        Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(BuiltInRegistries.ITEM.key(), id), item)

        return DecoEntry(
            id = id,
            block = block,
            item = item,
            transparent = transparent,
            parent = this
        ).also {
            innerChildren += it;
            ModDecoBlocks.children += it
            ModDecoBlocks.entries += this
        }
    }
    // endregion


    companion object {
        val children = mutableSetOf<DecoEntry<*>>()
        val entries = mutableSetOf<ModDecoBlocks>()

        fun register() {
            ModDecoBlocks::class.forceLoad()
        }
    }
}