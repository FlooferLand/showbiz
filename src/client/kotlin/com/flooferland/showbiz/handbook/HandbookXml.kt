package com.flooferland.showbiz.handbook
import net.minecraft.network.chat.*
import kotlinx.serialization.Serializable
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
    }

    @Serializable
    sealed class Element(val style: String = "") {
        @Serializable
        @XmlSerialName("container")
        data class Container(
            val type: String,
            val entries: List<Element> = emptyList()
        ) : Element()

        @Serializable
        @XmlSerialName("list")
        data class ListElement(
            val type: String,
            val lines: List<Line> = emptyList()
        ) : Element()

        @Serializable
        sealed class TextContent(
            val key: String = "",
            @XmlValue val text: String = ""
        ) : Element() {
            // TODO: Read the style property
            fun toComponent(): MutableComponent =
                Component.translatableWithFallback(key, text)
        }

        @Serializable
        @XmlSerialName("summary")
        class Summary : TextContent()

        @Serializable
        @XmlSerialName("p")
        class Paragraph : TextContent()

        @Serializable
        @XmlSerialName("l")
        class Line : TextContent()

        @Serializable
        @XmlSerialName("image")
        data class Image(val src: String, val alt: String = "") : Element()
    }
}