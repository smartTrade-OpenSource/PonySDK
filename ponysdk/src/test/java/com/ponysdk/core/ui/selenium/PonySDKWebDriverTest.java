package com.ponysdk.core.ui.selenium;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class PonySDKWebDriverTest {

    private static PonySDKWebDriver driver;
    private static WebDriverWait wait;

    @BeforeClass
    public static void beforeClass() {
        driver = new PonySDKWebDriver();
        wait = new WebDriverWait(driver, 30);
        driver.get("ws://localhost:8081/sample/ws?b=");
        boolean result;
        try {
            //driver.findElement(By.name("test")).click();

            result = wait.until((ExpectedCondition<Boolean>) webDriver -> {
                System.out.println("Searching ...");
                return webDriver.findElement(By.id("label")) != null;
            });
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            driver.close();
        }

        System.out.println("result final ... " + result);
    }

    @Test
    public void test(){

    }
}
