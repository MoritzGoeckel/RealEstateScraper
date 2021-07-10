package ingest.portals

import com.google.gson.GsonBuilder
import ingest.Downloader
import library.parseGermanDouble
import library.matches
import library.remove
import library.toCurrency
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import structures.*
import java.lang.Exception

class ImmoWelt : Downloader {
    override fun download(contract: Contract, page: Int): MutableList<Home>{
        // The ImmoWelt frontend makes per page:
        // 1 getPage request returning 4 homes and
        // 4 getListItems requests that each return 4 homes

        // TODO: Parameterize url to get a general entry point for parsing. Right now it is hardcoded for Stuttgart
        // TODO: Maybe also include houses, not just apartments

        val contractParam = when(contract){
            Contract.Buy -> "kaufen"
            Contract.Rent -> "mieten"
            Contract.None -> throw Exception("Bad contract type")
        }

        val homes = mutableListOf<Home>()
        val connection = getPage("liste/stuttgart/wohnungen/$contractParam", page)
        val doc = connection.get()

        doc.getElementsByClass("listitem")
            .map{parseHome(it, contract)}
            .forEach(homes::add)

        val listItemsQuery = doc.getElementById("filterView").attr("value")

        val response = connection.response()

        val homesPerPage = 4
        val homesPerGetList = 4
        val getListPerPage = 4
        val offset = page * ((getListPerPage * homesPerGetList) + homesPerPage)

        for(i in 4..4*4 step 4) {
            getListItems(response.cookies(), listItemsQuery, offset + i)
                .getElementsByClass("listitem")
                .map { parseHome(it, contract) }
                .forEach(homes::add)
        }

        return homes
    }

    private fun getListItems(cookies: Map<String, String>, query: String, offset: Int): Document {
        return Jsoup.connect("${BASEURL_IMMOWELT}liste/getlistitems")
            .method(Connection.Method.POST)
            .header("authority", """www.immowelt.de""")
            .header("sec-ch-ua", """" Not;A Brand";v="99", "Google Chrome";v="91", "Chromium";v="91"""")
            .header("accept", """text/plain, */*; q=0.01""")
            .header("x-requested-with", """XMLHttpRequest""")
            .header("sec-ch-ua-mobile", """?0""")
            .header("user-agent", """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36""")
            .header("content-type", """application/x-www-form-urlencoded; charset=UTF-8""")
            .header("origin", """https://www.immowelt.de""")
            .header("sec-fetch-site", """same-origin""")
            .header("sec-fetch-mode", """cors""")
            .header("sec-fetch-dest", """empty""")
            .header("accept-language", """en,de;q=0.9,de-DE;q=0.8,en-US;q=0.7""")
            .cookies(cookies)
            .data("query", query) // Something like: "geoid=10808111000&etype=1&esr=1,zip=70378&sort=relevanz"
            .data("offset", offset.toString())
            .data("pageSize", "4")
            .post()
    }


    private fun getPage(path: String, page: Int): Connection {
        return Jsoup.connect("$BASEURL_IMMOWELT$path?sort=createdate+desc&cp=$page")
            .header("authority", """www.immowelt.de""")
            .header("cache-control", """max-age=0""")
            .header("sec-ch-ua", """" Not;A Brand";v="99", "Google Chrome";v="91", "Chromium";v="91"""")
            .header("sec-ch-ua-mobile", """?0""")
            .header("upgrade-insecure-requests", """1""")
            .header("user-agent", """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36""")
            .header("accept", """text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9""")
            .header("sec-fetch-site", """same-origin""")
            .header("sec-fetch-mode", """navigate""")
            .header("sec-fetch-user", """?1""")
            .header("sec-fetch-dest", """document""")
            .header("referer", """https://www.immowelt.de/""")
            .header("accept-language", """en,de;q=0.9,de-DE;q=0.8,en-US;q=0.7""")
    }

    private fun parseHome(element: Element, contract: Contract): Home {
        val home = Home()
        parseTitle(element, home)
        parseLocation(element, home)
        parseFacts(element, home)
        parseUrl(element, home)
        parseImages(element, home)
        home.contract = contract
        home.type = Type.Offer
        // No description available
        return home
    }

    private fun parseTitle(element: Element, home: Home){
        home.title = element.getElementsByTag("h2")
            .map(Element::text)
            .reduce { acc, elem -> "$acc $elem" }
    }

    private fun parseLocation(element: Element, home: Home){
        var text = element.getElementsByClass("listlocation")
            .first()
            .text()

        val plzMatch = "[0-9]{5}".toRegex()
            .findAll(text)
            .firstOrNull()

        if(plzMatch != null){
            home.plz = plzMatch.value
            text = text.remove(plzMatch.value)
                .trim()
        }

        if(text.isNotEmpty()) home.address = text
    }

    private fun parseFacts(element: Element, home: Home){
        element.getElementsByClass("projectfact").forEach { parseFact(it, home) }
        element.getElementsByClass("hardfact").forEach { parseFact(it, home) }
    }

    private fun parseFact(element: Element, home: Home){
        // Facts can be in 'projectfacts' or in 'hardfacts' format.
        // So use regex to parse the text and disregard the structure
        // Maybe in the future: Look for 'objectfacts' instead of 'projectfacts'

        if(tryParseSize(element, home)) return
        if(tryParseRooms(element, home)) return
        if(tryParsePrice(element, home)) return
        throw Exception("Cant parse fact: " + element.text())
    }

    private fun tryParseRooms(element: Element, home: Home): Boolean{
        val text = element.text()
        if(!text.matches(".*(Zi\\.|Zimmer).*")) return false

        val numbers = parseNumbers("[1-9][0-9]*(\\.[0-9]*)?", text)

        when(numbers.size){
            0 -> return false
            1 -> home.rooms = numbers.first()
            2 -> {
                // This is a rooms range, we just take the maximum
                home.rooms = numbers.maxOrNull()!!
            }
            else -> return false
        }

        return true
    }

    private fun tryParseSize(element: Element, home: Home): Boolean{
        val text = element.text()
        if(!text.matches(".*m².*")) return false

        val numbers = parseNumbers("[1-9][0-9]*(,[0-9]*)?", text);

        when(numbers.size){
            0 -> return false
            1 -> home.squareMetres = numbers.first()
            2 -> {
                // This is a size range, we just take the maximum
                home.squareMetres = numbers.maxOrNull()!!
            }
            else -> return false
        }

        return true
    }

    private fun tryParsePrice(element: Element, home: Home): Boolean{
        val text = element.text()

        val currencies = "[€|$]".toRegex(RegexOption.IGNORE_CASE)
            .findAll(text)
            .toList()

        if(currencies.isEmpty()){
            return false
        }

        val price = Price()
        price.type = Price.Type.Normal // TODO is there 'VB' on ImmoWelt?

        val distinctCurrencies = currencies.map {it.value}.distinct()

        price.currency = if(distinctCurrencies.size > 1){
            Currency.Ambiguous
        } else {
            distinctCurrencies.first().toCurrency()
        }

        val numbers = parseNumbers("[1-9][0-9]*\\.+[0-9][0-9]*(,[0-9][0-9]*)?", text)

        when(numbers.size){
            0 -> return false
            1 -> {
                price.amount = numbers.first()
            }
            2 -> {
                // This is a price range, we just take the maximum
                price.amount = numbers.maxOrNull()!!
            }
            else -> return false
        }

        home.price = price

        return true
    }

    private fun parseNumbers(pattern: String, text: String): List<Double> {
        return pattern
            .toRegex()
            .findAll(text)
            .map(MatchResult::value)
            .map(String::parseGermanDouble)
            .filterNot(Double::isNaN)
            .toList()
    }

    private fun parseUrl(element: Element, home: Home){
        val links = element
            .getElementsByTag("a")
            .filter { it.hasAttr("href") }
            .map { it.attr("href") }
            .distinct()
            .filter { it.contains("expose") ||  it.contains("projekte") }

        when(links.size){
            0 -> throw Exception("No link found: $element")
            1 -> home.url = BASEURL_IMMOWELT + links.first()
            else -> throw Exception("More than one link found: $element")
        }
    }

    private fun parseImages(element: Element, home: Home){
        // Images are found in different structures depending
        // on the type of request. This covers both cases

        if(tryParseImagesSrcSet(element, home)) return
        if(tryParseImagesDataZsSrc(element, home)) return
        throw Exception("Could not parse images: $element")
    }

    private fun tryParseImagesSrcSet(element: Element, home: Home): Boolean {
        val images = element
            .getElementsByClass("img_center")
            .map { it.getElementsByAttribute("srcset").attr("srcset") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        home.images.addAll(images)
        return images.isNotEmpty()
    }

    private fun tryParseImagesDataZsSrc(element: Element, home: Home): Boolean {
        val jsonBlobs = element
            .getElementsByAttribute("data-zs-src")
            .map { it.attr("data-zs-src") }

        when(jsonBlobs.size){
            0 -> return false
            1 -> { /* Good case */ }
            else -> return false
        }

        val gson = GsonBuilder().create()
        var images = mutableListOf<String>()
        images = gson.fromJson<ArrayList<String>>(jsonBlobs.first(), images.javaClass)

        home.images.addAll(images.distinct())
        return images.isNotEmpty()
    }
}