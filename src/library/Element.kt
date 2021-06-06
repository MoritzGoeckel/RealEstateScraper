package library;

import org.jsoup.nodes.Element

fun Element.attributeOrNull(attribute: String): String?{
    return if (this.hasAttr(attribute) && this.attr(attribute).isNotEmpty()) {
        this.attr(attribute)
    } else {
        null
    }
}