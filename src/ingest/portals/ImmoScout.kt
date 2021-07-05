package ingest.portals

import ingest.Downloader
import library.parseGermanDouble
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import structures.Contract
import structures.Currency
import structures.Home
import structures.Price
import javax.swing.text.Document


class ImmoScout : Downloader {

    private val parseList: MutableList<(Home, Element) -> Home> = mutableListOf()

    override fun download(query: String, contract: Contract, page: Int): MutableList<Home> {

        val homeList = mutableListOf<Home>()

        collectParser()

        for (i in 1..page) {
            var doc = sendRequest(i)
            var elements = doc.getElementsByClass("result-list__listing")

           elements
               .map{parseData(it)}
               .toCollection(homeList)
        }
        return homeList
    }

    private fun parseData(element: Element) : Home {
        var home = Home()

        for(i in parseList) {
            home = i(home, element)
        }
        return home
    }

    private fun collectParser() {
        parseList.add { home: Home, element: Element ->
            home.title = element.getElementsByClass("result-list-entry__brand-title").text()
            home
        }

        parseList.add { home: Home, element: Element ->
            home.address = element.getElementsByClass("result-list-entry__map-link").text()
            home
        }

        parseList.add { home: Home, element: Element ->
            home.squareMeters = element.getElementsByClass("result-list-entry__primary-criterion")[1]
                .text()
                .split(" ")[0]
                .parseGermanDouble()
            home
        }

        parseList.add { home: Home, element: Element ->
            var price = Price()

            var priceElements = element.getElementsByClass("result-list-entry__primary-criterion")[0]
                .text()
                .split(" ")

            price.amount = priceElements[0].parseGermanDouble()
            price.currency = parseCurrency(priceElements[1])
            home.price = price
            home
        }

        parseList.add { home: Home, element: Element ->
            home.rooms = element.getElementsByClass("result-list-entry__primary-criterion")[2]
                .text()
                .split(" ")[0]
                .parseGermanDouble()
            home
        }

        parseList.add { home: Home, element: Element ->
            home.images.add(element.getElementsByClass("gallery__image")[0].attributes().get("src"))
            home
        }

        parseList.add { home: Home, element: Element ->
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
                """seastate="TGFzdFNlYXJjaA==:ZmFsc2UsMTYyNTQ4MTE2NDE0MSwvZGUvd29obnVuZy1taWV0ZW4="; feature_ab_tests="TouchPointPictureShape@2=Default|CWV@13=Default|IF311@2=NEW|ADS2470@3=TestGroup"; ssoOptimizelyUniqueVisitorId=17599ec0-e384-475d-ab24-8a7e7de73f78; is24_experiment_visitor_id=2e28a94e-4a4c-4417-bced-503439a1a795; longUnreliableState="dWlkcg==:YS0xMzVlMDkwOTQyNTA0MTU3YmE4ODM2ZGFjY2M2MmQ5Nw=="; ABNTEST=1624107190; reese84=3:/PgtFzRlxAv+QVMQlLi3tg==:4Joeh8fS0YaTh/mR1Xi0uaBwee/secHFWS5wszIaCtI1PJYew0CFedpAQBb+R7Of4Eo0997KjHm+KnU6iOn/8YSDpgMeyMz7ZxUwK45qxLI36C3Q7MzfqJR2+eactP0SZEJRXO55BtgQrmDbBthQsUPFMqn78IXbeGMBXjrcJqi3SHT8DANzBt1vf9JPyY22q/EQFw/ERaqxsdGu13qjEW1/LJhjIoLxqsv12mgfekFU1ApHU87Jqm+zmO2TtrktcJZfS8VJ8+chLlL2/CxVLHFRqGQrrQRtJVbKpGNM9jrxbeKMSw0EXU0DmJ7kPnRiSp8iLtX01R7hDcU5bX/o46JYJiHHL3vuemTXYnJT5qURB51JjdA1TOjHYcaB2ok1Ftt9r46XCpddpZ4Schr4suspSPoEvims5Cf0BSjQX2g=:qobmkYRiJw50ToiyTOn37V1MrwUN2JFLRVJbwV6+iT0="""
            )
            .header("sec-fetch-user", "?1")
            .get()
    }

    private fun parseCurrency(currencyAsString : String): Currency {

        return when(currencyAsString) {
            "€" -> Currency.EUR
            "$" -> Currency.USD
            else -> Currency.Other
        }
    }


}