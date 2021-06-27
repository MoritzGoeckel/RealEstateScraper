package tests

import structures.Contract
import ingest.Downloader
import ingest.portals.Ebay
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import com.google.gson.GsonBuilder
import ingest.portals.ImmoScout
import ingest.portals.ImmoWelt

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
        val homes = downloader.download(Contract.Buy, 6)
        val avgFaultiness = homes.sumOf { it.faultiness() } / homes.size

        homes.map(::prettyPrint)
             .map(::println)

        assertTrue { avgFaultiness < 0.05 }
        assertTrue { homes.size >= 20 }
    }

    @Test
    fun immoScoutDownloader(){
        val downloader: Downloader = ImmoScout()
        val homes = downloader.download(Contract.Buy, 6)

        homes.map(::prettyPrint)
            .map(::println)

        // TODO: Faultiness related to missing plz
        assertTrue { homes.size >= 20 }
    }

    @Test
    fun immoWeltDownloader(){
        val downloader: Downloader = ImmoWelt()
        val homes = downloader.download(Contract.Buy, 6)
        val avgFaultiness = homes.sumOf { it.faultiness() } / homes.size

        homes.map(::prettyPrint)
            .map(::println)

        assertTrue { avgFaultiness < 0.10 }
        assertTrue { homes.size >= 20 }
    }
}
