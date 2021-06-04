package ingest.portals

import ingest.*
import structures.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

private fun String.remove(str: String): String{
    return this.replace(str, "", true)
}

private fun parseDouble(input: String?): Double?{
    if(input == null) return null

    return input.remove(".")
        .replace(",", ".")
        .trim()
        .toDoubleOrNull()
}

private fun attributeOrNull(element: Element, attribute: String): String?{
    return if (element.hasAttr(attribute) && element.attr(attribute).isNotEmpty()) {
        element.attr(attribute)
    } else {
        null
    }
}

class Ebay : Downloader {
    private val baseUrl = "https://www.ebay-kleinanzeigen.de/"

    private fun priceFromString(str: String) : Price {
        val price = Price()

        val currencies = ArrayList<Currency>()

        var priceStr = str
        if(priceStr.contains('€')) {
            currencies.add(Currency.EUR)
            priceStr = priceStr.remove("€")
                               .trim()
        }

        if(priceStr.contains('$')) {
            currencies.add(Currency.USD)
            priceStr = priceStr.remove("$")
                               .trim()
        }

        if(currencies.size > 1) price.currency = Currency.Ambiguous
        if(currencies.isEmpty()) price.currency = Currency.Other
        else price.currency = currencies.first()

        if(priceStr.contains(" VB", true)){
            price.type = Price.Type.Negotiable
            priceStr = priceStr.remove(" VB")
                .trim()
        } else {
            price.type = Price.Type.Normal
        }

        if(priceStr == "Zu verschenken"){
            price.amount = 0.0
        } else {
            price.amount = parseDouble(priceStr)
        }

        return price
    }

    private fun parsePrice(element: Element, home: Home){
        home.price = element.getElementsByClass("aditem-main--middle--price")
            .asSequence()
            .map(Element::text)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(this::priceFromString)
            .firstOrNull() { it.amount != null }
    }

    private fun parseTitle(element: Element, home: Home){
        home.title = element.getElementsByClass("ellipsis")
                            .joinToString(" | ", transform = Element::text)

        if(home.title.isNullOrEmpty()) home.title = null
    }

    private fun parseUrl(element: Element, home: Home){
        home.url = element.getElementsByClass("ellipsis")
                          .mapNotNull {  attributeOrNull(it, "href") }
                          .map { baseUrl + it }
                          .firstOrNull()
    }

    private fun parseAddress(element: Element, home: Home){
        element.getElementsByClass("aditem-main--top--left")
            .map(Element::text)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map {
                var str = it

                val plzMatch = "[0-9]{5}".toRegex()
                                         .findAll(str)
                                         .firstOrNull()

                if(plzMatch != null){
                    home.plz = plzMatch.value
                    str = str.remove(plzMatch.value)
                             .trim()
                }

                if(str.isNotEmpty()) home.address = str
            }
    }

    private fun parseDescription(element: Element, home: Home){
        home.description = element.getElementsByClass("aditem-main--middle--description")
                                  .joinToString(" | ", transform = Element::text)

        if(home.description.isNullOrEmpty()) home.description = null
    }

    private fun parseTags(element: Element, home: Home){
        element.getElementsByClass("simpletag")
                .map(Element::text)
                .map{
                    when {
                        it.contains("Gesuch", true) -> {
                            home.type = Type.Ask
                        }
                        it.endsWith("m²") -> {
                            home.squareMeters = parseDouble(it.remove("m²"))
                        }
                        it.endsWith("Zimmer") -> {
                            home.rooms = parseDouble(it.remove("Zimmer"))
                        }
                    }
                }

        if(home.type == null) home.type = Type.Offer
    }

    private fun parseImages(element: Element, home: Home){
        home.images.addAll(
            element.getElementsByClass("imagebox")
                   .mapNotNull { attributeOrNull(it, "data-imgsrc") }
        )
    }


    override fun download(query: String, contract: Contract, page: Int): MutableList<Home>{
        // TODO: Query is not used

        var prefix = ""
        var postfix = ""
        when (contract) {
            Contract.Buy -> {
                prefix = "s-haus-kaufen"
                postfix = "c208"
            }
            Contract.Rent -> {
                prefix = "s-haus-mieten"
                postfix = "c205"
            }
        }

        val url = "$baseUrl$prefix/seite:$page/$postfix"

        val doc = Jsoup.connect(url)
                       .get()

        val homes = doc.getElementsByClass("ad-listitem")
            .map {
                val home = Home()
                home.contract = contract
                parsePrice(it, home)
                parseAddress(it, home)
                parseTitle(it, home)
                parseUrl(it, home)
                parseDescription(it, home)
                parseTags(it, home)
                parseImages(it, home)
                home
            }
            .filter { !it.isFaulty() }

        return homes.toMutableList()
    }
}
