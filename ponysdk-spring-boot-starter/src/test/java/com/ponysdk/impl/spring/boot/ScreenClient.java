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

package com.ponysdk.impl.spring.boot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.driver.PonyFrame;
import com.ponysdk.driver.PonyMessageListener;
import com.ponysdk.driver.PonySDKWebDriver;
import com.ponysdk.driver.PonySessionListener;
import com.ponysdk.driver.PonyWebElement;

import jakarta.json.JsonObject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

/** A browser's side of a screen, headless: the Pony driver on the WebSocket, with what it saw to wait for. */
final class ScreenClient {

    private final List<Object> texts = new CopyOnWriteArrayList<>();
    private final CountDownLatch destroyed = new CountDownLatch(1);
    private final CountDownLatch closed = new CountDownLatch(1);
    private final PonySDKWebDriver driver;

    /** Opens the WebSocket of the application at {@code url}, e.g. {@code ws://127.0.0.1:8080/app/ws}. */
    ScreenClient(final String url) {
        driver = new PonySDKWebDriver(new PonyMessageListener() {

            @Override
            public void onSendMessage(final JsonObject message) {
            }

            @Override
            public void onReceiveMessage(final List<PonyFrame> message) {
                final Object text = PonySDKWebDriver.findValueForModel(message, ServerToClientModel.TEXT);
                if (text != null) texts.add(text);
                if (message.stream().anyMatch(frame -> frame.getModel() == ServerToClientModel.DESTROY_CONTEXT)) destroyed.countDown();
            }
        }, null, new PonySessionListener() {

            @Override
            public void onOpen(final Session session) {
            }

            @Override
            public void onClose(final Session session, final CloseReason closeReason) {
                closed.countDown();
            }

            @Override
            public void onError(final Session session, final Throwable thr) {
            }
        }, true);
        driver.get(url);
    }

    /** True once a widget with this text was sent: the first screen is there. */
    boolean seen(final String text) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!texts.contains(text) && System.nanoTime() < deadline) Thread.sleep(20);
        return texts.contains(text);
    }

    void click(final String tag) {
        driver.findElement(By.tagName(tag)).click();
    }

    PonyWebElement element(final String tag) {
        return driver.findElement(By.tagName(tag));
    }

    /** The server told the client its context is over: it reloads its page. */
    boolean awaitDestroyed() throws InterruptedException {
        return destroyed.await(10, TimeUnit.SECONDS);
    }

    boolean awaitClosed() throws InterruptedException {
        return closed.await(10, TimeUnit.SECONDS);
    }

    void close() {
        driver.close();
    }

    void assertSeen(final String text) throws InterruptedException {
        assertTrue(seen(text), "the screen sent " + text + ", saw " + texts);
    }
}
