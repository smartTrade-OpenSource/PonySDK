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

package com.ponysdk.core.ui.basic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.HasPClickHandlers;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.eventbus.StreamHandler;
import com.ponysdk.core.writer.ModelWriter;

/**
 * A widget that displays the image at a given URL. The image can be in 'unclipped' mode (the
 * default) or 'clipped' mode.
 * In clipped mode, a viewport is overlaid on top of the image so that a subset of the image will be
 * displayed.
 * In unclipped mode, there is no viewport - the entire image will be visible.
 * Whether an image is in clipped or unclipped mode depends on how the image is constructed, and how
 * it is transformed after construction.
 * Methods will operate differently depending on the mode that the image is in.
 * These differences are detailed in the documentation for each method.
 * <p>
 * If an image transitions between clipped mode and unclipped mode, any {@link PElement}-specific
 * attributes added by the user (including style attributes, style names, and style modifiers),
 * except for eventbus listeners, will be lost.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-Image</dt>
 * <dd>The outer element</dd>
 * </dl>
 * Transformations between clipped and unclipped state will result in a loss of any style names that
 * were set/added; the only style names that are preserved are those that are mentioned in the
 * static CSS style rules.
 * Due to browser-specific HTML constructions needed to achieve the clipping effect, certain CSS
 * attributes, such as padding and background, may not work as expected when an image is in clipped
 * mode. These limitations can usually be easily worked around by encapsulating the image in a
 * container widget that can itself be styled.
 */
public class PImage extends PWidget implements HasPClickHandlers {

    private static final Logger log = LoggerFactory.getLogger(PImage.class);
    private final int left;
    private final int top;
    private final int imageWidth;
    private final int imageHeight;
    private String url;

    protected PImage() {
        this((String) null);
    }

    protected PImage(final ClassPathURL classpathURL) {
        this(classpathURL.toUrl());
    }

    protected PImage(final String url) {
        this(url, -1, -1, -1, -1);
    }

    protected PImage(final String url, final int left, final int top, final int width, final int height) {
        super();
        this.url = url;
        this.left = left;
        this.top = top;

        this.imageWidth = width; // TODO nicolas reuse widget width
        this.imageHeight = height;// TODO nicolas reuse widget height;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        if (url != null) {
            writer.write(ServerToClientModel.URL, url);
            if (top != -1 && left != -1 && imageHeight != -1 && imageWidth != -1) {
                writer.write(ServerToClientModel.POSITION_LEFT, left);
                writer.write(ServerToClientModel.POSITION_TOP, top);
                writer.write(ServerToClientModel.IMAGE_HEIGHT, imageHeight);
                writer.write(ServerToClientModel.IMAGE_WIDTH, imageWidth);
            }
        }
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.IMAGE;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        if (Objects.equals(this.url, url)) return;
        this.url = url;
        if (initialized) saveUpdate(ServerToClientModel.URL, url);
    }

    public void setStream(final StreamHandler streamListener) {
        UIContext.get().stackEmbeddedStreamRequest(streamListener, this);
    }

    @Override
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    public static final class ClassPathURL {

        private final URL url;

        public ClassPathURL(final String resourcePath) {
            url = getClass().getClassLoader().getResource(resourcePath);
        }

        final String toUrl() {
            String imageToBase64 = null;

            try (InputStream in = url.openStream()) {
                final byte[] buffer = new byte[1024];
                try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    while (in.read(buffer) != -1) {
                        out.write(buffer);
                    }
                    imageToBase64 = new String(out.toByteArray(), "UTF-8");
                }
            } catch (final IOException e) {
                log.error("Cannot load resource from " + this, e);
            }

            final String extension = url.getFile().substring(url.getFile().lastIndexOf('.') + 1);

            return "data:image/" + extension + ";base64," + imageToBase64;
        }

        @Override
        public String toString() {
            return "ClassPathURL [url=" + url + "]";
        }
    }

}
