package ingest.portals

import ingest.Downloader
import library.parseGermanDouble
import library.toCurrency
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import structures.Contract
import structures.Currency
import structures.Home
import structures.Price
import java.time.Duration


class ImmoScout : Downloader {

    constructor() {
        collectParser()
    }

    private val parseMap: MutableMap<String,(Home, Element) -> Home> = mutableMapOf()

    override fun download(query: String, contract: Contract, page: Int): MutableList<Home> {

        var doc = sendRequest(page)
        var elements = doc.getElementsByClass("result-list__listing")

        return elements
            .map{parse(it)}
            .toCollection(mutableListOf())

    }

    private fun parse(element: Element) : Home {
        var home = Home()

        for(currentParser in parseMap.entries) {
            home = currentParser.value(home, element)
        }
        return home
    }

    private fun collectParser() {
        parseMap["title"] = { home: Home, element: Element ->
            home.title = element.getElementsByClass("result-list-entry__brand-title").text()
            home
        }

        parseMap["address"] = { home: Home, element: Element ->
            home.address = element.getElementsByClass("result-list-entry__map-link").text()
            home
        }

        parseMap["squareMetres"] = { home: Home, element: Element ->
            home.squareMetres = element.getElementsByClass("result-list-entry__primary-criterion")[1]
                .text()
                .split(" ")
                .first()
                .parseGermanDouble()
            home
        }

        parseMap["price"] = { home: Home, element: Element ->

            var price = Price()
            var priceElements = element.getElementsByClass("result-list-entry__primary-criterion")
                .first()
                .text()
                .split(" ")

            price.amount = priceElements.first().parseGermanDouble()
            price.currency = priceElements[1].toCurrency()
            home.price = price
            home
        }

        parseMap["rooms"] = { home: Home, element: Element ->
            home.rooms = element.getElementsByClass("result-list-entry__primary-criterion")[2]
                .text()
                .split(" ")
                .first()
                .parseGermanDouble()
            home
        }

        parseMap["images"] = { home: Home, element: Element ->
            home.images.add(element.getElementsByClass("gallery__image").attr("src"))
            home
        }

        parseMap["url"] = { home: Home, element: Element ->
            home.url = BASEURL_IMMOSCOUT + element.getElementsByClass("result-list-entry__brand-title-container").attr("href")
            home
        }
    }


    private fun sendRequest(page: Int): org.jsoup.nodes.Document {
        val url = "${BASEURL_IMMOSCOUT}Suche/de/wohnung-mieten?pagenumber=$page"

        return Jsoup.connect(url)
            .header("sec-ch-ua", """ Not;A Brand";v="99", "Google Chrome";v="91", "Chromium";v="91"""")
            .header("sec-ch-ua-mobile", "?0")
            .header("upgrade-insecure-requests", "1")
            .header(
                "user-agent",
                """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.106 Safari/537.36"""
            )
            .header(
                "accept",
                """text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"""
            )
            .header("sec-fetch-site", "none")
            .header("sec-fetch-mode", "navigate")
            .header(
                "cookie",
                """seastate="TGFzdFNlYXJjaA==:ZmFsc2UsMTYyNTY1ODE4NDA4MiwvZGUvd29obnVuZy1taWV0ZW4="; feature_ab_tests="CWV@14=Default|IF311@2=NEW|ADS2470@3=TestGroup|SEA2587@2=ON"; ssoOptimizelyUniqueVisitorId=17599ec0-e384-475d-ab24-8a7e7de73f78; is24_experiment_visitor_id=2e28a94e-4a4c-4417-bced-503439a1a795; longUnreliableState="dWlkcg==:YS0xMzVlMDkwOTQyNTA0MTU3YmE4ODM2ZGFjY2M2MmQ5Nw=="; ABNTEST=1624107190; reese84=3:bDmdTaKxEODlEGJPsrmGYQ==:sxJ0Meb81BV1o+WhMG3gDLawJ+ZDnwTivoYrWi8Y3fxD/bLMHePx4DZnD41CpfKP4w1qVFlFQQ/ukv09Y0ByV3UjAp+OXTCq81R8cxBR3dWU5EQuvSanKg4aSgS+KnTI72q9v69QKIN57b4uHUPJrXPYS0dUJP9pGQTBbAOz5MFT8PJCIAYSjh75nhlxLVr+Hz24Qm+/gvjhSpVuxVSKtv9Rn0nUv3pc9qCePjthle329Z8bokMLEmFQFsDWY6iYaSC4EwXca5+9QpU6dNLBsu8obAUKn9UeBdyTz0Y/na0/D0EnsqaT+1M8LESzCnGh3qHAJ/A/JUsLZVuMi3KCZ9VWRp/Zmcz41E+aLKI5qde293cSvpBdOho0rMBnU8flI5fFT5WGiZuB1NCOkW0zvR5yZFQKxwIFhPB5sWUg/FI=:cMmxJxnqnQmL5tpsHA/xz1CG9rSTqMiJ02EYI2a9GHs="""
            )
            .header("sec-fetch-user", "?1")
            .get()
    }

}