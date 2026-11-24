package com.flooferland.showbiz.handbook
import com.flooferland.showbiz.types.math.Vec2i
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
        val pages: Collections.Pages = Collections.Pages(),

        @XmlElement(true)
        val styles: Collections.Styles = Collections.Styles(),
    )

    @Serializable
    @XmlSerialName("head")
    data class Head(
        @XmlElement(true)
        val summary: Element.Summary,

        @XmlElement(true)
        val facts: Collections.Facts = Collections.Facts()
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
    object Collections {
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

        @Serializable @XmlSerialName("fit")
        data class Fit(val type: String) : Styling() {
            companion object {
                const val COVER_TYPE = "cover"
            }
        }
    }

    @Serializable
    sealed class Element(val style: String = "") {
        @Transient internal var owner: HandbookEntry? = null

        abstract fun getSize(): Vec2i
        fun getStyles(): StyleDefs {
            return owner?.styles[style] ?: StyleDefs(id = style)
        }

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
                val comp = if (key.isNotEmpty()) {
                    Component.translatableWithFallback(key, text)
                } else {
                    Component.literal(text)
                }
                return Handbook.Style.getComp(comp, getStyles())
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