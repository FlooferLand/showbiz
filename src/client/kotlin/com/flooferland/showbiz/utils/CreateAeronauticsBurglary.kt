package com.flooferland.showbiz.utils

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.mixin.accessor.CreativeModeInventoryScreenAccessor
import com.flooferland.showbiz.registry.ModItemGroups
import com.flooferland.showbiz.types.math.Color4
import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import java.util.function.Consumer
import org.joml.Vector3f


object CreateAeronauticsBurglary {
    private const val ITEMS_PER_ROW = 9

    data class SectionData(val name: String, val info: Component, val sprite: ResourceLocation, val color: Color4, val secondaryColor: Color4? = null)
    val sections = mapOf(
        ModItemGroups.Section.Main to SectionData(
            name = "Main", info = Component.literal("Functional, etc"),
            sprite = rl("main_banner"), color = Color4(201, 151, 42),
        ),
        ModItemGroups.Section.Deco to SectionData(
            name = "Deco", info = Component.literal("Building blocks"),
            sprite = rl("deco_banner"), color = Color4(201, 151, 42),
        )
    )

    @JvmField var CURRENT_ROW = 0
    val SECTION_Y_VALUES = Object2IntOpenHashMap<ModItemGroups.Section>()
    private val SECTION_ITEM_COUNTS = mutableListOf<Int>()

    @JvmStatic
    fun renderBanners(screen: CreativeModeInventoryScreen, graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val accessor = (screen as? CreativeModeInventoryScreenAccessor) ?: return
        val pose = graphics.pose()
        pose.pushPose()

        RenderSystem.enableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        val left = accessor.getLeftPos() + 8
        val top = accessor.getTopPos() + 17
        pose.translate(left.toFloat(), top.toFloat(), 0f)

        for ((id, section) in sections) {
            val yValue = SECTION_Y_VALUES.getInt(id)
            val sectionRow = (yValue - CURRENT_ROW)
            if (sectionRow !in 0..4) continue

            val font: Font = Minecraft.getInstance().font
            val w = 162
            val h = 18
            val x = 0
            val y = sectionRow * h

            val bannerTexture = section.sprite
            graphics.blitSprite(bannerTexture, x, y, w, h)

            val isHovering = mouseX in (left + x..left + x + w) && mouseY in (top + y..top + y + h)

            val text = Component.literal(section.name)
            if (isHovering) text.append(" - ").append(section.info)
            val textWidth = font.width(text)

            val background = section.color.withOpacity(0.6f).pack()
            graphics.fill(x + 2, y + 2, x + textWidth + 8, y + h - 2, background)

            val light = section.color
            val dark = section.secondaryColor ?: light.darken(0.2f)
            drawAuraText(graphics, text, dark.pack(), light.pack(), x + 5, y + 5)
        }
        pose.popPose()
        RenderSystem.disableDepthTest()
    }

    fun drawAuraText(graphics: GuiGraphics, text: Component, color1: Int, color2: Int, x: Int, y: Int) {
        val font = Minecraft.getInstance().font
        val window = Minecraft.getInstance().getWindow()
        val scale = window.getGuiScale().toFloat()

        graphics.drawString(font, text, x, y, color1, true)

        val ps = graphics.pose()
        ps.pushPose()
        ps.translate(0f, 0f, 1f)
        val pose = ps.last().copy().pose()
        val position = pose.transformPosition(Vector3f(x.toFloat(), y.toFloat(), 0f))
        val corner = pose.transformPosition(Vector3f((x + font.width(text)).toFloat(), y + font.lineHeight / 1.8f, 0f))

        position.mul(scale)
        corner.mul(scale)
        val height = (corner.y - position.y).toInt()
        val width = (corner.x - position.x).toInt()
        RenderSystem.enableScissor(
            position.x.toInt(),
            window.getHeight() - position.y.toInt() - height,
            width,
            height
        )
        graphics.drawString(font, text, x, y, color2, false)
        RenderSystem.disableScissor()
        ps.popPose()
    }

    @JvmStatic
    fun processItems(displayItems: Consumer<ItemStack?>, searchItems: Consumer<ItemStack>) {
        val sectionMap = HashMap<ModItemGroups.Section, MutableList<ItemStack>>()

        for (stack in ModItemGroups.Main.items) {
            val sectionId = ModItemGroups.Main.sections[stack] ?: continue
            sectionMap.computeIfAbsent(sectionId) { s -> mutableListOf() }.add(stack)
        }

        SECTION_Y_VALUES.clear()
        SECTION_ITEM_COUNTS.clear()

        var y = 0
        val sectionKeys = sectionMap.keys.stream().sorted().toList();
        for (key in sectionKeys) {
            var itemCount = 0
            val sectionItems: MutableList<ItemStack> = sectionMap[key]!!

            for (item in sectionItems) {
                //var item = item
                displayItems.accept(item)
                searchItems.accept(item)
                /*item = CreativeTabItemTransforms.applyTransform(item)

                if (CreativeTabItemTransforms.VisibilityType.SEARCH_ONLY.has(item.getItem())) {
                    searchItems.accept(item)
                } else if (!CreativeTabItemTransforms.VisibilityType.INVISIBLE.has(item.getItem())) {
                    displayItems.accept(item)
                    searchItems.accept(item)
                    itemCount++
                }*/
                itemCount++
            }

            SECTION_Y_VALUES.put(key, y)
            SECTION_ITEM_COUNTS.add(itemCount)
            val rowCount = Math.ceilDiv(itemCount, ITEMS_PER_ROW)
            y += rowCount + 1
        }
    }

    @JvmStatic
    fun padMenuItems(items: MutableList<ItemStack?>) {
        if (SECTION_ITEM_COUNTS.isEmpty()) return

        var expectedItemCount = 0
        for (sectionItemCount in SECTION_ITEM_COUNTS) {
            expectedItemCount += sectionItemCount
        }

        if (items.size != expectedItemCount) return

        val padded = ObjectArrayList<ItemStack>()
        addEmptySlots(padded, ITEMS_PER_ROW)

        var itemIndex = 0
        for ((sectionIndex, element) in SECTION_ITEM_COUNTS.withIndex()) {
            val sectionItemCount = element
            val nextItemIndex = itemIndex + sectionItemCount
            padded.addAll(items.subList(itemIndex, nextItemIndex))
            itemIndex = nextItemIndex

            if (sectionIndex < SECTION_ITEM_COUNTS.size - 1) {
                val slotsToFinishRow = (ITEMS_PER_ROW - sectionItemCount % ITEMS_PER_ROW) % ITEMS_PER_ROW
                addEmptySlots(padded, slotsToFinishRow + ITEMS_PER_ROW)
            }
        }

        items.clear()
        items.addAll(padded)
    }

    private fun addEmptySlots(items: MutableList<ItemStack>, count: Int) {
        for (i in 0..<count) {
            items.add(ItemStack.EMPTY)
        }
    }
}