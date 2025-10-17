package com.flooferland.showbiz.registry.blocks

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.blockPath
import kotlinx.serialization.json.JsonObject
import net.minecraft.resources.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.properties.*

/** Class I use for data generation, for more complex things.
 * Can / should be implemented by the blocks themselves
 */
interface CustomBlockModel {
    fun modelData(): JsonObject? = null
    fun modelBlockStates(builder: BlockStateBuilder, blockId: ResourceLocation) {}
    fun modelBlockStates(builder: BlockStateBuilder) {}

    @DslMarker
    annotation class CustomBlockModelDsl

    class NameBuilder(ctx: BlockStateBuilder, name: String?, val postfix: String?) {
        var name: String? = null
        override fun toString(): String = name ?: ""
        init {
            if (name != null) {
                this.name = name
            } else if (postfix != null) {
                this.name = "${ctx.blockId.path}_${postfix}"
            }
        }
    }

    @CustomBlockModelDsl
    class BlockStateBuilder(val blockId: ResourceLocation, val block: Block) {
        constructor(block: ModBlocks) : this(block.id, block.block)

        val states = mutableListOf<Variation>()
        private var defaultStateValue: Comparable<*>? = null
        private var defaultStateProp: StrippedProperty? = null
        private var buildFinished = false

        fun finish() {
            if (buildFinished) return
            if (states.isEmpty()) {
                val defaultProperty = defaultStateProp?.let { block.stateDefinition.getProperty(it.name) } ?: block.stateDefinition.properties.firstOrNull()
                if (defaultProperty == null) {
                    Showbiz.log.error("No properties found via ${BlockStateBuilder::class.simpleName} for $blockId")
                    return
                }
                val defaultValue = runCatching { block.defaultBlockState().getValue(defaultProperty) }.getOrNull()
                    ?: defaultProperty.possibleValues.first()

                val name = NameBuilder(this, name = blockId.path, postfix = "")
                val stateModel = StateModel(this, name)
                val defaultProp = StrippedProperty(block, defaultProperty)
                val variation = Variation(this, defaultProp, defaultValue, stateModel, name)
                states.add(variation)
                this.defaultStateProp = defaultProp
                this.defaultStateValue = defaultValue
            }
            buildFinished = true
        }

        // TODO: Add a better way to get the default block state by looping through the block
        //       Only supports one block state rn
        fun getDefaultState(): StateModel {
            if (!buildFinished) finish()
            val found = states.firstOrNull({ state -> state.prop.name == defaultStateProp?.name })
            return found?.state ?: states.firstOrNull()!!.state
        }

        /** Set the default block state */
        fun <T: Comparable<T>> defaultState(prop: Property<T>, value: T) {
            defaultStateValue = value
            defaultStateProp = StrippedProperty(block, prop)
        }

        // Bool
        @CustomBlockModelDsl
        class BoolStateScope(val ctx: BlockStateBuilder, val prop: BooleanProperty) {
            public fun trueState(name: String? = null, postfix: String? = null, block: StateModel.() -> Unit) {
                val name = NameBuilder(ctx, name = name, postfix = postfix)
                val built = StateModel(ctx, name).apply(block)
                ctx.states.add(Variation(
                    ctx, StrippedProperty(ctx.block, prop),
                    expected = true,
                    state = built,
                    name = name
                ))
            }
            public fun falseState(name: String? = null, postfix: String? = null, block: StateModel.() -> Unit) {
                val name = NameBuilder(ctx, name = name, postfix = postfix)
                val built = StateModel(ctx, name).apply(block)
                ctx.states.add(Variation(
                    ctx, StrippedProperty(ctx.block, prop),
                    expected = false,
                    state = built,
                    name = name
                ))
            }
        }
        fun bool(prop: BooleanProperty, block: BoolStateScope.() -> Unit) {
            BoolStateScope(this, prop).apply(block)
        }
    }

    /** Type erasure for the normal *Property classes */
    @CustomBlockModelDsl
    class StrippedProperty(val name: String, val default: Any) {
        constructor(block: Block, prop: Property<*>) : this(prop.name, block.defaultBlockState().getValue(prop))
    }

    /** A block state variation. Stores a name, a value, and the block state */
    @CustomBlockModelDsl
    data class Variation(
        public val ctx: BlockStateBuilder,
        public val prop: StrippedProperty,
        public val expected: Any,
        public val state: StateModel,
        public val name: NameBuilder
    )

    class StateModel(val ctx: BlockStateBuilder, val name: NameBuilder) {
        var model: Model? = null
        fun model(block: Model.() -> Unit) = apply { this.model = Model(ctx).apply(block) }
    }

    @CustomBlockModelDsl
    class Model(val ctx: BlockStateBuilder) {
        public val textures = mutableListOf<ResourceLocation>()
        private fun addTex(tex: ResourceLocation) = textures.add(tex.blockPath())

        fun texture(tex: ResourceLocation) = apply { addTex(tex) }
        fun endTextureWith(suffix: String?) = apply {
            if (suffix == null) {
                addTex(ctx.blockId)
                return@apply
            }
            addTex(ctx.blockId.withSuffix("_${suffix}"))
        }
    }
}