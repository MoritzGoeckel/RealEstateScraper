import downloader.Kleinanzeigen
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Tests {
    fun main() {
        println("Hello world!")
    }

    @Test
    fun someThing(){
        assertEquals("0", "1");
    }

    @Test
    fun someOtherThing(){
        val kleinanzeigen = Kleinanzeigen()
        kleinanzeigen.download()
    }
}
