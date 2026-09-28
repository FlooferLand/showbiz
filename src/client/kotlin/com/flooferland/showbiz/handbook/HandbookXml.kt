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
        val summary: Types.Summary,

        @XmlSerialName("fact")
        val facts: List<Types.Fact> = emptyList(),

        @XmlSerialName("page")
        val page: List<Page> = emptyList(),
    )

    @Serializable
    @XmlSerialName("page")
    data class Page(
        val title: String? = null,
        val entries: List<Types.PageElement> = emptyList(),
    )

    object Types {
        @Serializable
        sealed class PageElement()

        @Serializable
        sealed class TextContent(
            @XmlValue val text: String = "",
            val key: String = ""
        ) : PageElement() {
            fun toComponent(): MutableComponent =
                Component.translatableWithFallback(key, text)
        }

        @Serializable
        @XmlSerialName("vbox")
        data class VBoxContainer(val entries: List<Types.PageElement> = emptyList()) : PageElement()

        @Serializable
        @XmlSerialName("fact")
        class Fact : TextContent()

        @Serializable
        @XmlSerialName("summary")
        class Summary : TextContent()

        @Serializable
        @XmlSerialName("p")
        class Paragraph : TextContent()

        @Serializable
        @XmlSerialName("image")
        data class Image(val src: String, val alt: String = "") : PageElement()
    }
}