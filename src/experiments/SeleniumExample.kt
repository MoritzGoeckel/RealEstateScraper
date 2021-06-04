package experiments

import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class SeleniumExample {
    fun main() {
        val driver = FirefoxDriver()
        val wait = WebDriverWait(driver, Duration.ofSeconds(10).toMillis())
        try {
            driver.get("https://google.com/ncr")
            driver.findElement(By.name("q")).sendKeys("cheese" + Keys.ENTER)
            val firstResult = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("h3")))
            println(firstResult.getAttribute("textContent"))
        } finally {
            driver.quit()
        }
        // https://www.selenium.dev/documentation/en/
    }
}
