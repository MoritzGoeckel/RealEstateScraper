package downloader

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class Kleinanzeigen {
    fun main() {
        val driver = FirefoxDriver()
        val wait = WebDriverWait(driver, Duration.ofSeconds(10).toMillis())
        try {
            driver.get("https://google.com/ncr")
            driver.findElement(By.name("q")).sendKeys("cheese" + Keys.ENTER)
            val firstResult = wait.until(presenceOfElementLocated(By.cssSelector("h3")))
            println(firstResult.getAttribute("textContent"))
        } finally {
            driver.quit()
        }
        // https://www.selenium.dev/documentation/en/
    }
}