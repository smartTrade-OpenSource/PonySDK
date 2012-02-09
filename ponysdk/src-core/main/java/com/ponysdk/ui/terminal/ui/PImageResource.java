
package com.ponysdk.ui.terminal.ui;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;

public class PImageResource implements ImageResource {

    private final String uri;

    private final int width;

    private final int height;

    public PImageResource(final String uri, final int width, final int height) {
        this.uri = uri;
        this.width = width;
        this.height = height;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getLeft() {
        return 0;
    }

    @Override
    public SafeUri getSafeUri() {
        return new SafeUri() {

            @Override
            public String asString() {
                return uri;
            }
        };
    }

    @Override
    public int getTop() {
        return 0;
    }

    @Override
    public String getURL() {
        return uri;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public boolean isAnimated() {
        return false;
    }

}
