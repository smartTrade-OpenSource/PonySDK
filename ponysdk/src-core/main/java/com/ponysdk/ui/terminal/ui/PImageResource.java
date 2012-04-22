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
