import downloader.Contract
import downloader.Downloader
import downloader.Kleinanzeigen
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class Tests {
    @Test
    fun kleinanzeigenDownloader(){
        val downloader: Downloader = Kleinanzeigen()
        val homes = downloader.download("", Contract.Buy, 6)
        val avgFaultiness = homes.sumOf { it.faultiness() } / homes.size

        assertTrue { avgFaultiness < 0.1 }
        assertTrue { homes.size >= 25 }
    }
}
