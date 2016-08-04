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

package com.ponysdk.core.ui.rich;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PImage;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PPopupPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.alignment.PHorizontalAlignment;
import com.ponysdk.core.ui.basic.event.PClickEvent;

public class PClosableDialogBox extends PPopupPanel {

    private final PSimplePanel captionContainer;

    private final PSimplePanel closeContainer;

    private final PSimplePanel contentContainer;

    public PClosableDialogBox(final int windowID, final String caption) {
        this(windowID, false, new PLabel(caption), new PImage("images/close_16.png"));
    }

    public PClosableDialogBox(final int windowID, final boolean modal, final IsPWidget captionWidget, final IsPWidget closeWidget) {
        super(windowID, false);
        setModal(modal);

        setStyleName("pony-closable-dialog-box");

        captionContainer = new PSimplePanel();
        closeContainer = new PSimplePanel();
        contentContainer = new PSimplePanel();
        captionContainer.setStyleName("caption");
        closeContainer.setStyleName("close");
        contentContainer.setStyleName("content");

        final PFlexTable layout = new PFlexTable();
        layout.addStyleName("layout");

        layout.setWidget(0, 0, captionContainer);
        layout.setWidget(0, 1, closeContainer);
        layout.setWidget(1, 0, contentContainer);

        layout.getCellFormatter().setColSpan(1, 0, 2);
        layout.getCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_LEFT);
        layout.getCellFormatter().setHorizontalAlignment(0, 1, PHorizontalAlignment.ALIGN_RIGHT);
        layout.getRowFormatter().addStyleName(0, "header");

        closeContainer.addDomHandler((PClickEvent) -> hide(), PClickEvent.TYPE);

        super.setWidget(layout);

        captionContainer.setWidget(captionWidget);
        closeContainer.setWidget(closeWidget);
    }

    public void setCaption(final IsPWidget widget) {
        captionContainer.setWidget(widget);
    }

    public void setClose(final IsPWidget widget) {
        closeContainer.setWidget(widget);
    }

    public void setContent(final IsPWidget widget) {
        contentContainer.setWidget(widget);
    }

    @Override
    public void setWidget(final PWidget w) {
        throw new IllegalArgumentException("Use PClosableDialogBox.setContent() to set the content of the popup");
    }

    public void displayAtCenter() {
        setPopupPositionAndShow((offsetWidth, offsetHeight, windowWidth,
                windowHeight) -> setPopupPosition((windowWidth - offsetWidth) / 2, (windowHeight - offsetHeight) / 2));
    }
}
