import downloader.Contract
import downloader.Downloader
import downloader.Ebay
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class Tests {
    @Test
    fun ebayDownloader(){
        val downloader: Downloader = Ebay()
        val homes = downloader.download("", Contract.Buy, 6)
        val avgFaultiness = homes.sumOf { it.faultiness() } / homes.size

        assertTrue { avgFaultiness < 0.05 }
        assertTrue { homes.size >= 20 }
    }
}
