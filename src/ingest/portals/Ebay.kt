package ingest.portals

import ingest.*
import library.*
import structures.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.lang.RuntimeException

class Ebay : Downloader {

    private fun priceFromString(str: String) : Price {
        val price = Price()

        val currencies = mutableListOf<Currency>()

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
            price.amount = priceStr.parseGermanDouble()
        }

        return price
    }

    private fun parsePrice(element: Element, home: Home){
        home.price = element.getElementsByClass("aditem-main--middle--price")
            .map(Element::text)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(this::priceFromString)
            .firstOrDefault(::Price)
    }

    private fun parseTitle(element: Element, home: Home){
        home.title = element.getElementsByClass("ellipsis")
                            .joinToString(" | ", transform = Element::text)
    }

    private fun parseUrl(element: Element, home: Home){
        home.url = element.getElementsByClass("ellipsis")
                          .mapNotNull {  it.attributeOrNull("href") }
                          .map { BASEURL_EBAY + it }
                          .firstOrDefault { "" }
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
                            home.squareMetres = it.remove("m²")
                                                  .parseGermanDouble()
                        }
                        it.endsWith("Zimmer") -> {
                            home.rooms = it.remove("Zimmer")
                                           .parseGermanDouble()
                        }
                    }
                }

        if(home.type == Type.None) home.type = Type.Offer
    }

    private fun parseImages(element: Element, home: Home){
        home.images.addAll(
            element.getElementsByClass("imagebox")
                   .mapNotNull { it.attributeOrNull("data-imgsrc") }
        )
    }

    override fun download(contract: Contract, page: Int): MutableList<Home>{
        val prefix: String
        val postfix: String
        when (contract) {
            Contract.Buy -> {
                prefix = "s-haus-kaufen"
                postfix = "c208"
            }
            Contract.Rent -> {
                prefix = "s-haus-mieten"
                postfix = "c205"
            }
            else -> throw RuntimeException("Unexpected contract type")
        }

        val url = "$BASEURL_EBAY$prefix/anzeige:angebote/seite:$page/$postfix"

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
            .filter(Home::isValid)

        return homes.toMutableList()
    }
}
