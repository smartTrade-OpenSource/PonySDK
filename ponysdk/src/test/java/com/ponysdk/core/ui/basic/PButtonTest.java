/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ponysdk.core.model.WidgetType;

public class PButtonTest extends PSuite {

    @Test
    public void testInit() {
        final PButton widget = new PButton();
        assertEquals(WidgetType.BUTTON, widget.getWidgetType());
        assertNotNull(widget.toString());
    }

    @Test
    public void testSetText() {
        final PButton widget = new PButton("", "HTML");
        assertEquals("", widget.getText());
        assertEquals("HTML", widget.getHTML());
        widget.setText("Text");
        assertEquals("Text", widget.getText());
        assertNull(widget.getHTML());
    }

    @Test
    public void testSetHtml() {
        final PButton widget = new PButton("Text");
        assertEquals("Text", widget.getText());
        assertNull(widget.getHTML());
        widget.setHTML("HTML");
        assertEquals("HTML", widget.getHTML());
        assertNull(widget.getText());
    }

    @Test
    public void testSetEnabled() {
        final PButton widget = new PButton();
        assertTrue(widget.isEnabled());
        widget.setEnabled(false);
        assertFalse(widget.isEnabled());
        widget.setEnabled(true);
        assertTrue(widget.isEnabled());
    }

    @Test
    public void testSetEnabledOnRequest() {
        final PButton widget = new PButton();
        assertFalse(widget.isEnabledOnRequest());
        widget.setEnabledOnRequest(false);
        assertFalse(widget.isEnabledOnRequest());
        widget.setEnabledOnRequest(true);
        assertTrue(widget.isEnabledOnRequest());
    }

    @Test
    public void testShowLoadingOnRequest() {
        final PButton widget = new PButton();
        assertFalse(widget.isShowLoadingOnRequest());
        widget.showLoadingOnRequest(false);
        assertFalse(widget.isShowLoadingOnRequest());
        widget.showLoadingOnRequest(true);
        assertTrue(widget.isShowLoadingOnRequest());
    }

}
