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

package com.ponysdk.ui.server.basic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.StreamResource;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Update;

public class PImage extends PFocusWidget {

    private static Logger log = LoggerFactory.getLogger(PImage.class);

    private String url;

    public PImage() {}

    public PImage(String url) {
        setUrl(url);
    }

    public PImage(final ClassPathURL classpathURL) {

        InputStream in = null;
        ByteArrayOutputStream out = null;

        String imageToBase64 = null;

        try {
            in = classpathURL.getUrl().openStream();
            final byte[] buffer = new byte[1024];
            out = new ByteArrayOutputStream();
            while (in.read(buffer) != -1) {
                out.write(buffer);
            }

            imageToBase64 = new String(out.toByteArray(), "UTF-8");

        } catch (final IOException e) {
            log.error("Cannot load resource from " + classpathURL, e);
        } finally {
            try {
                in.close();
            } catch (final Exception e) {}
            try {
                out.close();
            } catch (final Exception e) {}
        }

        final String extension = classpathURL.getUrl().getFile().substring(classpathURL.getUrl().getFile().lastIndexOf('.') + 1);

        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.IMAGE_URL, "data:image/" + extension + ";base64," + imageToBase64);
        getPonySession().stackInstruction(update);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.IMAGE;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.IMAGE_URL, url);
        getPonySession().stackInstruction(update);
    }

    public void setStream(StreamHandler streamListener) {
        final StreamResource streamResource = new StreamResource();
        streamResource.embed(streamListener, this);
    }

    public static class ClassPathURL {

        private final URL url;

        public ClassPathURL(String resourcePath) {
            url = getClass().getClassLoader().getResource(resourcePath);
        }

        public URL getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "ClassPathURL [url=" + url + "]";
        }
    }

}
