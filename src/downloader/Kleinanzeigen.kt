package downloader

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Kleinanzeigen : Downloader{

    private val baseUrl = "https://www.ebay-kleinanzeigen.de/"

    private fun parseDouble(input: String?): Double?{
        if(input == null) return null

        var str = input.replace(".", "")
        str = str.replace(",", ".")
        str = str.trim()
        return str.toDoubleOrNull()
    }

    private fun attributeOrNull(element: Element, attribute: String): String?{
        return if (element.hasAttr(attribute) && element.attr(attribute).isNotEmpty()) {
            element.attr(attribute)
        } else {
            null
        }
    }

    private fun parsePrice(element: Element, home: Home){
        val prices = ArrayList<Price>()
        for(priceElement in element.getElementsByClass("aditem-main--middle--price")) {
            var str = priceElement
                .text()
                .trim()

            if(str.isEmpty()) continue

            val price = Price()
            prices.add(price)

            val currencies = ArrayList<Currency>()

            if(str.contains('€')) {
                currencies.add(Currency.EUR)
                str = str.replace("€", "")
            }

            if(str.contains('$')) {
                currencies.add(Currency.USD)
                str = str.replace("$", "")
            }

            if(currencies.size > 1) price.currency = Currency.Ambiguous
            if(currencies.isEmpty()) price.currency = Currency.Other
            else price.currency = currencies.first()

            str = str.trim()

            if(str.contains(" VB", true)){
                price.type = Price.Type.Negotiable
                str = str.replace(" VB", "", true)
                str = str.trim()
            } else {
                price.type = Price.Type.Normal
            }

            if(str == "Zu verschenken"){
                price.amount = 0.0
                continue
            }

            price.amount = parseDouble(str)
        }

        if(prices.size == 1){
            home.price = prices.first()
            return
        }

        if(prices.size > 1){
             home.price = prices.firstOrNull { it.amount != null }
        }
    }

    private fun parseTitle(element: Element, home: Home){
        home.title = element.getElementsByClass("ellipsis").joinToString(" | ") { it.text() }
        if(home.title.isNullOrBlank()) home.title = null
    }

    private fun parseUrl(element: Element, home: Home){
        home.url = element.getElementsByClass("ellipsis")
            .mapNotNull {  attributeOrNull(it, "href") }
            .map { baseUrl + it }
            .firstOrNull()
    }

    private fun parseAddress(element: Element, home: Home){
        for(addressElement in element.getElementsByClass("aditem-main--top--left")){
            var str = addressElement
                .text()
                .trim()

            if(str.isEmpty()) continue

            val foundPlz = "[0-9]{5}".toRegex().findAll(str).firstOrNull()
            if(foundPlz != null){
                home.plz = foundPlz.value
                str = str.replace(foundPlz.value, "")
                str = str.trim()
            }

            if(str.isNotEmpty()) home.address = str
        }
    }

    private fun parseDescription(element: Element, home: Home){
        home.description = element.getElementsByClass("aditem-main--middle--description")
                            .joinToString(" | ") { it.text() }

        if(home.description.isNullOrEmpty()) home.description = null
    }

    private fun parseTags(element: Element, home: Home){
        element.getElementsByClass("simpletag")
        .map{ it.text() }
        .map{
            when {
                it.contains("Gesuch", true) -> {
                    home.type = Type.Ask
                }
                it.endsWith("m²") -> {
                    home.squareMeters = parseDouble(it.replace("m²", ""))
                }
                it.endsWith("Zimmer") -> {
                    home.rooms = parseDouble(it.replace("Zimmer", ""))
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

        val doc = Jsoup
            .connect(url)
            .get()

        val items = doc.getElementsByClass("ad-listitem")

        val homes = ArrayList<Home>()
        for(item in items){
            val home = Home()
            home.contract = contract
            parsePrice(item, home)
            parseAddress(item, home)
            parseTitle(item, home)
            parseUrl(item, home)
            parseDescription(item, home)
            parseTags(item, home)
            parseImages(item, home)

            if(home.isFaulty()) continue

            homes.add(home)
        }

        return homes
    }
}
