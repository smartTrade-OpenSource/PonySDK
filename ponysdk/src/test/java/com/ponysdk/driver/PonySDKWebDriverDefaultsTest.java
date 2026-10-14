package com.ponysdk.driver;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

/** The no-arg driver must not hand a null bandwidth listener to its deflate extension. */
public class PonySDKWebDriverDefaultsTest {

    @Test
    public void defaultConstructorWiresANonNullBandwidthListenerIntoTheDeflateExtension() throws Exception {
        // Regression: the constructor passed the (null) parameter instead of the defaulted field, so the
        // extension NPE'd on the first compressed frame and the server closed with "RSV bit(s) incorrectly set".
        final PonySDKWebDriver driver = new PonySDKWebDriver();
        final Field clientField = PonySDKWebDriver.class.getDeclaredField("client");
        clientField.setAccessible(true);
        final Object client = clientField.get(driver);
        final Field extensionsField = client.getClass().getDeclaredField("extensions");
        extensionsField.setAccessible(true);
        final Object extension = ((List<?>) extensionsField.get(client)).get(0);
        final Field listenerField = PonyDriverPerMessageDeflateExtension.class.getDeclaredField("listener");
        listenerField.setAccessible(true);
        assertNotNull(listenerField.get(extension));
    }
}
