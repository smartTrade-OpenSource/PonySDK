
package com.ponysdk.ui.terminal.ui;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.user.client.ui.Image;

public class PImageResource implements ImageResource {

    private final String uri;

    private final int width;

    private final int height;

    private final int left;

    private final int top;

    public PImageResource(final Image image) {
        this.uri = image.getUrl();
        this.left = image.getOriginLeft();
        this.top = image.getOriginTop();
        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    public PImageResource(final String uri, final int left, final int top, final int width, final int height) {
        this.uri = uri;
        this.left = left;
        this.top = top;
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
        return left;
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
        return top;
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
