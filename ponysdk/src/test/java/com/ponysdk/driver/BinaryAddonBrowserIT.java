/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.driver;

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Optional, non-blocking real-browser check for the binary PAddOn protocol. It drives a headless
 * Chrome against a running PonySDK showcase and asserts that the <strong>GWT terminal</strong> — the
 * one protocol decoder that exists only as compiled JavaScript and therefore cannot be unit-tested
 * in the JVM — correctly decoded the typed binary creation arguments and a 1000-element binary
 * method-call array on the "Binary" tab.
 * <p>
 * Skipped unless {@code -Dponysdk.browserTest=true} is set, so it never runs (or fails) in the
 * normal build. Optional system properties:
 * <ul>
 * <li>{@code -Dponysdk.browserUrl} — showcase URL (default {@code http://localhost:8081/sample/})</li>
 * <li>{@code -Dponysdk.chromeBinary} — explicit Chrome/Chromium binary (else the system Chrome)</li>
 * </ul>
 * The dedicated, non-required CI workflow starts the server first, then runs this test.
 */
public class BinaryAddonBrowserIT {

    private WebDriver driver;

    @Before
    public void onlyWhenEnabled() {
        Assume.assumeTrue("set -Dponysdk.browserTest=true to run", Boolean.getBoolean("ponysdk.browserTest"));

        final ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage",
            "--window-size=1280,900");
        final String binary = System.getProperty("ponysdk.chromeBinary");
        if (binary != null && !binary.isBlank()) options.setBinary(binary);
        driver = new ChromeDriver(options);
    }

    @After
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test
    public void terminalDecodesBinaryCreationArgsAndLargeArray() {
        driver.get(System.getProperty("ponysdk.browserUrl", "http://localhost:8081/sample/"));

        final WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        // Open the "Binary" tab (button label is "≡  Binary")
        wait.until(d -> d.findElement(By.xpath("//button[contains(normalize-space(.), 'Binary')]"))).click();
        // Wait until the addon has decoded the binary method array (the demo JS sets window.__bigSum)
        wait.until(d -> ((JavascriptExecutor) d).executeScript("return window.__bigSum") != null);

        final JavascriptExecutor js = (JavascriptExecutor) driver;
        final long createLen = ((Number) js.executeScript("return window.__createLen")).longValue();
        final long bigLen = ((Number) js.executeScript("return window.__bigLen")).longValue();
        final long bigSum = ((Number) js.executeScript("return window.__bigSum")).longValue();

        assertEquals("typed binary creation args delivered to the factory", 5, createLen);
        assertEquals("1000-element binary method array (beyond the old 255 cap)", 1000, bigLen);
        assertEquals("sum of 0..999 decoded intact by the GWT terminal", 499_500, bigSum);
    }
}
