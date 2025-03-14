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

import com.ponysdk.core.model.WidgetType;

/**
 * A standard button widget.
 * <h2>CSS Style Rules</h2>
 * <dl>
 * <dt>.gwt-Button</dt>
 * <dd>the outer element</dd>
 * </dl>
 *
 * @see com.google.gwt.user.client.ui.Button
 */
public class PButton extends PButtonBase {

    /**
     * Instantiates a new PButton
     */
    protected PButton() {
        super();
    }

    /**
     * Instantiates a new PButton
     *
     * @param text
     *            the text
     */
    protected PButton(final String text) {
        super(text);
    }

    /**
     * Instantiates a new PButton
     *
     * @param text
     *            the text
     * @param html
     *            the html
     */
    protected PButton(final String text, final String html) {
        super(text, html);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.BUTTON;
    }

}
