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
package com.ponysdk.ui.terminal.addon.dialogbox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PCDialogBox extends DialogBox {

    private final HorizontalPanel captionPanel = new HorizontalPanel();

    private Widget closeWidget = new Anchor("close");
    private boolean closable = true;

    private final HTML text = new HTML("&nbsp;");

    public PCDialogBox() {
        super();
        buildGUI();
    }

    @Override
    public void setHTML(String html) {
        text.setHTML(html);
    }

    @Override
    public void setHTML(SafeHtml html) {
        this.text.setHTML(html);
    }

    @Override
    public void setText(String text) {
        this.text.setHTML(text);
    }

    private void buildGUI() {
        closeWidget.setVisible(closable);
        captionPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        captionPanel.setWidth("99%");
        captionPanel.add(text);
        captionPanel.add(closeWidget);
        captionPanel.setCellHorizontalAlignment(closeWidget, HasHorizontalAlignment.ALIGN_RIGHT);
        // tiny close area to not close by clicking by error
        captionPanel.setCellWidth(closeWidget, "1%");
        captionPanel.addStyleName("Caption");

        // Erase GWT implementation
        final Element td = getCellElement(0, 1);
        td.setInnerHTML("");
        td.appendChild(captionPanel.getElement());
    }

    protected boolean isCaptionControlEvent(NativeEvent event) {
        return isWidgetEvent(event, closeWidget);
    }

    /**
     * Overrides the browser event from the DialogBox
     */
    @Override
    public void onBrowserEvent(Event event) {
        if (isCaptionControlEvent(event)) {

            switch (event.getTypeInt()) {
            case Event.ONMOUSEUP:
            case Event.ONCLICK:
                hide();
                break;
            case Event.ONMOUSEOVER:
                break;
            case Event.ONMOUSEOUT:
                break;
            }

            return;
        }

        // go to the DialogBox browser event
        super.onBrowserEvent(event);
    }

    protected boolean isWidgetEvent(NativeEvent event, Widget w) {
        final EventTarget target = event.getEventTarget();

        if (com.google.gwt.dom.client.Element.is(target)) {
            final boolean t = w.getElement().isOrHasChild(com.google.gwt.dom.client.Element.as(target));
            GWT.log("isWidgetEvent:" + w + ':' + target + ':' + t);
            return t;
        }
        return false;
    }

    public void setCloseWidget(Widget closeWidget) {
        this.closeWidget = closeWidget;
        captionPanel.remove(1);
        captionPanel.insert(closeWidget, 1);
        captionPanel.setCellHorizontalAlignment(closeWidget, HasHorizontalAlignment.ALIGN_RIGHT);
        closeWidget.setVisible(closable);
    }

    public void setClosable(boolean closable) {
        this.closable = closable;
        closeWidget.setVisible(closable);
    }
}
