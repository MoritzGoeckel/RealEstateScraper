package tests

import structures.Contract
import ingest.Downloader
import ingest.portals.Ebay
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import com.google.gson.GsonBuilder

fun prettyPrint(any: Any): String {
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeSpecialFloatingPointValues()
        .create()

    return gson.toJson(any)
}

class Tests {
    @Test
    fun ebayDownloader(){
        val downloader: Downloader = Ebay()
        val homes = downloader.download("", Contract.Buy, 6)
        val avgFaultiness = homes.sumOf { it.faultiness() } / homes.size

        homes.map(::prettyPrint)
             .map(::println)

        println(homes.size)

        assertTrue { avgFaultiness < 0.05 }
        assertTrue { homes.size >= 20 }
    }
}
