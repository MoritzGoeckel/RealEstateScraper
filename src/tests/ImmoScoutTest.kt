package tests

import ingest.Downloader
import ingest.portals.Ebay
import ingest.portals.ImmoScout
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import structures.Contract
import kotlin.contracts.contract

internal class ImmoScoutTest {

    @Test
    fun ebayDownloader(){
        val downloader: Downloader = ImmoScout()
        val homes = downloader.download("", Contract.Buy, 6)

        homes.map(::prettyPrint)
            .map(::println)

        println(homes.size)

        kotlin.test.assertTrue { homes.size >= 20 }
    }
}