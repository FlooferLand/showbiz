package com.flooferland.showbiz.handbook
import com.flooferland.showbiz.types.math.Vec2i
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

object HandbookXml {
    @Serializable
    @XmlSerialName("root", "showbiz:handbook")
    data class Root(
        @XmlElement(true)
        val head: Head,

        @XmlElement(true)
        val pages: Transient.Pages = Transient.Pages(),

        @XmlElement(true)
        val styles: Transient.Styles = Transient.Styles(),
    )

    @Serializable
    @XmlSerialName("head")
    data class Head(
        @XmlElement(true)
        val summary: Element.Summary,

        @XmlElement(true)
        val facts: Transient.Facts = Transient.Facts()
    )

    @Serializable
    @XmlSerialName("page")
    data class Page(
        val title: String? = null,
        val entries: List<Element> = emptyList(),
    )

    @Serializable
    @XmlSerialName("def")
    data class StyleDefs(
        val id: String,
        val entries: List<Styling> = emptyList(),
    )

    /// Wrappers since XML parsing won't work another way
    object Transient {
        @Serializable
        @XmlSerialName("facts")
        data class Facts(
            val lines: List<Element.Line> = emptyList(),
        )
        @Serializable
        @XmlSerialName("pages")
        data class Pages(
            val entries: List<Page> = emptyList(),
        )
        @Serializable
        @XmlSerialName("styles")
        data class Styles(
            val entries: List<StyleDefs> = emptyList(),
        )
    }

    @Serializable
    sealed class Styling {
        @Serializable @XmlSerialName("foreground")
        data class Foreground(val color: String) : Styling()

        @Serializable @XmlSerialName("background")
        data class Background(val color: String) : Styling()
    }

    @Serializable
    sealed class Element(val style: String = "") {
        abstract fun getSize(): Vec2i

        protected fun textSize(text: Component) = Vec2i(
            x = Minecraft.getInstance()?.font?.width(text) ?: 0,
            y = Minecraft.getInstance()?.font?.lineHeight ?: 0
        )
        protected fun containerSize(entries: List<Element>): Vec2i {
            val size = Vec2i()
            for (item in entries) {
                size += item.getSize()
            }
            return size
        }

        @Serializable
        @XmlSerialName("container")
        data class Container(
            val type: String,
            val entries: List<Element> = emptyList()
        ) : Element() {
            override fun getSize() = containerSize(entries)
        }

        @Serializable
        @XmlSerialName("list")
        data class ListElement(
            val type: String,
            val lines: List<Line> = emptyList()
        ) : Element() {
            override fun getSize() = containerSize(lines)
        }

        @Serializable
        sealed class TextContent(
            val key: String = "",
            @XmlValue val text: String = ""
        ) : Element() {
            override fun getSize() = textSize(toComp())

            // TODO: Read the style property
            fun toComp(): MutableComponent {
                val styles = Handbook.cache.styles[style] ?: StyleDefs(id = style)
                val comp = if (key.isNotEmpty()) {
                    Component.translatableWithFallback(key, text)
                } else {
                    Component.literal(text)
                }
                return Handbook.Style.getComp(comp, styles)
            }
        }

        @Serializable
        @XmlSerialName("summary")
        class Summary(
            val entries: List<Element> = emptyList()
        ) : Element() {
            override fun getSize() = containerSize(entries)
        }

        @Serializable
        @XmlSerialName("p")
        class Paragraph : TextContent()

        @Serializable
        @XmlSerialName("l")
        class Line : TextContent()

        @Serializable
        @XmlSerialName("image")
        data class Image(val src: String, val alt: String = "") : Element() {
            override fun getSize(): Vec2i {
                // TODO: Measure texture sizes
                return Vec2i.ZERO
            }
        }
    }
}