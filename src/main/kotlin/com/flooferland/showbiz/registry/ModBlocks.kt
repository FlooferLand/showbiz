package com.flooferland.showbiz.registry

import com.flooferland.showbiz.blocks.GreyboxBlock
import com.flooferland.showbiz.blocks.ReelToReelBlock
import com.flooferland.showbiz.blocks.SpeakerBlock
import com.flooferland.showbiz.blocks.StagedBotBlock
import com.flooferland.showbiz.blocks.entities.GreyboxBlockEntity
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.blocks.entities.SpeakerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.datagen.providers.BlockProvider.BlockModelId
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.BlockBehaviour.*
import com.flooferland.showbiz.blocks.CurtainBlock
import com.flooferland.showbiz.blocks.CurtainControllerBlock
import com.flooferland.showbiz.blocks.CurtainShadowBlock
import com.flooferland.showbiz.blocks.ShowParserBlock
import com.flooferland.showbiz.blocks.ShowSelectorBlock
import com.flooferland.showbiz.blocks.BitViewBlock
import com.flooferland.showbiz.blocks.CymbalBlock
import com.flooferland.showbiz.blocks.PlushBlock
import com.flooferland.showbiz.blocks.ProgrammerBlock
import com.flooferland.showbiz.blocks.SpotlightBlock
import com.flooferland.showbiz.blocks.base.FancyBlockItem
import com.flooferland.showbiz.blocks.entities.BitViewBlockEntity
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.blocks.entities.CurtainControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.CymbalBlockEntity
import com.flooferland.showbiz.blocks.entities.PlushBlockEntity
import com.flooferland.showbiz.blocks.entities.ShowParserBlockEntity
import com.flooferland.showbiz.blocks.entities.ProgrammerBlockEntity
import com.flooferland.showbiz.blocks.entities.ShowSelectorBlockEntity
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.items.PlushBlockItem
import com.flooferland.showbiz.items.base.GeoBlockItem

// TODO: Add requiresCorrectToolForDrops and set up JSON tags for it to actually work

enum class ModBlocks {
    StagedBot(
        "staged_bot", ::StagedBotBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion(),
        entity = Entity(::StagedBotBlockEntity, isGeckolib = false),
    ),
    ReelToReel(
        "reel_to_reel", ::ReelToReelBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom.transparent(),
        entity = Entity(::ReelToReelBlockEntity, isGeckolib = false),
    ),
    Greybox(
        "greybox", ::GreyboxBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom.transparent(),
        entity = Entity(::GreyboxBlockEntity, isGeckolib = false),
    ),
    Speaker(
        "speaker", ::SpeakerBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = Entity(::SpeakerBlockEntity, isGeckolib = false),
    ),
    ShowParser(
        "show_parser", ::ShowParserBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = Entity(::ShowParserBlockEntity, isGeckolib = false),
    ),
    ShowSelector(
        "show_selector", ::ShowSelectorBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = Entity(::ShowSelectorBlockEntity, isGeckolib = true),
    ),
    CurtainBlock(
        "curtain_block", ::CurtainBlock,
        Properties.of()
            .strength(0.3f)
            .sound(SoundType.WOOL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = Entity(::CurtainBlockEntity, isGeckolib = false),
    ),
    CurtainBlockShadow(
        "curtain_block_shadow", ::CurtainShadowBlock,
        Properties.of()
            .strength(100f)
            .sound(SoundType.EMPTY)
            .replaceable()
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        hideFromPlayer = true,
    ),
    CurtainController(
        "curtain_controller", ::CurtainControllerBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.STONE)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = Entity(::CurtainControllerBlockEntity, isGeckolib = false),
    ),
    WojackBlock(
        "wojack_block", ::Block,
        Properties.of()
        .strength(5f)
        .sound(SoundType.AMETHYST)
        .noOcclusion(),
        modelPreset = BlockModelId.CubeAll,
        hideFromPlayer = true
    ),
    Spotlight(
        "spotlight", ::SpotlightBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = Entity(::SpotlightBlockEntity, isGeckolib = true),
    ),
    BitView(
        "bit_view", ::BitViewBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = Entity(::BitViewBlockEntity, isGeckolib = false),
    ),
    Programmer(
        "programmer", ::ProgrammerBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = Entity(::ProgrammerBlockEntity, isGeckolib = false),
    ),
    Plush(
        "plush", ::PlushBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.WOOL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        item = ::PlushBlockItem,
        entity = Entity(::PlushBlockEntity, isGeckolib = true),
        hideFromPlayer = true
    ),
    Cymbal(
        "cymbal", ::CymbalBlock,
        Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion(),
        modelPreset = BlockModelId.Custom,
        entity = Entity(::CymbalBlockEntity, isGeckolib = true),
    )
    ;

    val id: ResourceLocation
    val block: Block
    val item: BlockItem
    var model: BlockModelId? = null
    var entityType: BlockEntityType<*>? = null
    var isGeckoLib: Boolean = false
    var hideFromPlayer: Boolean = false
    constructor(name: String, constructor: (Properties) -> Block, props: Properties, item: BlockItemConstructor? = null, modelPreset: BlockModelId = BlockModelId.CubeAll, entity: Entity? = null, hideFromPlayer: Boolean = false) {
        this.id = rl(name)
        this.model = modelPreset;
        this.hideFromPlayer = hideFromPlayer
        this.isGeckoLib = entity?.isGeckolib == true

        this.block = Blocks.register(
            ResourceKey.create(BuiltInRegistries.BLOCK.key(), this.id),
            //? if >1.21.9 {
            /*constructor, props
            *///?} else {
            constructor(props)
            //?}
        )

        // Item
        var blockItem = item?.invoke(this.id, this.block, Item.Properties()) ?: when {
            isGeckoLib -> GeoBlockItem(this.id, this.block, Item.Properties())
            else -> FancyBlockItem(this.id, this.block, Item.Properties())
        }
        this.item = Items.registerBlock(blockItem) as BlockItem

        // Entity
        if (entity != null && !DataGenerator.engaged) {
            this.entityType = BlockEntityType.Builder.of(entity.entity, this.block).build()
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, this.id, this.entityType!!)
        }
    }

    typealias BlockItemConstructor = (ResourceLocation, Block, Item.Properties) -> BlockItem
    data class Entity(val entity: ((pos: BlockPos, blockState: BlockState) -> BlockEntity), val isGeckolib: Boolean)
}