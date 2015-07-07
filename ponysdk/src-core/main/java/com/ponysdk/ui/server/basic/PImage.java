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
import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A widget that displays the image at a given URL. The image can be in 'unclipped' mode (the default) or
 * 'clipped' mode. In clipped mode, a viewport is overlaid on top of the image so that a subset of the image
 * will be displayed. In unclipped mode, there is no viewport - the entire image will be visible. Whether an
 * image is in clipped or unclipped mode depends on how the image is constructed, and how it is transformed
 * after construction. Methods will operate differently depending on the mode that the image is in. These
 * differences are detailed in the documentation for each method.
 * <p>
 * If an image transitions between clipped mode and unclipped mode, any {@link PElement}-specific attributes
 * added by the user (including style attributes, style names, and style modifiers), except for event
 * listeners, will be lost.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-Image</dt></dd>The outer element</dd>
 * </dl>
 * Transformations between clipped and unclipped state will result in a loss of any style names that were
 * set/added; the only style names that are preserved are those that are mentioned in the static CSS style
 * rules. Due to browser-specific HTML constructions needed to achieve the clipping effect, certain CSS
 * attributes, such as padding and background, may not work as expected when an image is in clipped mode.
 * These limitations can usually be easily worked around by encapsulating the image in a container widget that
 * can itself be styled.
 */
public class PImage extends PFocusWidget {

    private static Logger log = LoggerFactory.getLogger(PImage.class);

    private String url;

    private int left;

    private int top;

    public PImage() {}

    public PImage(final String url, final int left, final int top, final int width, final int height) {
        create.put(PROPERTY.WIDGET_HEIGHT, width);
        create.put(PROPERTY.WIDGET_WIDTH, height);
        create.put(PROPERTY.IMAGE_URL, url);
        create.put(PROPERTY.IMAGE_LEFT, left);
        create.put(PROPERTY.IMAGE_TOP, top);
    }

    public PImage(final String url) {
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
                if (in != null) in.close();
            } catch (final Exception e) {}
            try {
                if (out != null) out.close();
            } catch (final Exception e) {}
        }

        final String extension = classpathURL.getUrl().getFile().substring(classpathURL.getUrl().getFile().lastIndexOf('.') + 1);

        final Update update = new Update(getID());
        update.put(PROPERTY.IMAGE_URL, "data:image/" + extension + ";base64," + imageToBase64);
        Txn.get().getTxnContext().save(update);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.IMAGE;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
        final Update update = new Update(getID());
        update.put(PROPERTY.IMAGE_URL, url);
        Txn.get().getTxnContext().save(update);
    }

    public void setStream(final StreamHandler streamListener) {
        final StreamResource streamResource = new StreamResource();
        streamResource.embed(streamListener, this);
    }

    public static class ClassPathURL {

        private final URL url;

        public ClassPathURL(final String resourcePath) {
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

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

}
