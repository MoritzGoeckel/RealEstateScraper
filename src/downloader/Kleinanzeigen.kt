package downloader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

class Kleinanzeigen {

    private fun makeSingleString(elems: Elements): String{
        if(elems.size == 0) return "None"
        return elems.joinToString(" | ");
    }

    fun download(){
        val doc = Jsoup
            .connect("https://www.ebay-kleinanzeigen.de/s-haus-kaufen/seite:3/c208")
            .get()

        val items = doc.getElementsByClass("ad-listitem")

        println(doc.title())
        for(item in items){
            val home = Home();

            home.title = makeSingleString(item.getElementsByClass("ellipsis"))

            val price = makeSingleString(item.getElementsByClass("aditem-main--middle--price"))
            // home.price = // parse price

            home.description = makeSingleString(item.getElementsByClass("aditem-main--middle--description"))
            home.address = makeSingleString(item.getElementsByClass("aditem-main--top--left"))

            val tags = item.getElementsByClass("simpletag") // Get square meters
            val images = item.getElementsByClass("imagebox") // -> img -> src
        }

    }

    fun main(){
        download()
    }
}
