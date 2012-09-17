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

package com.ponysdk.ui.server.form.renderer;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PGrid;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PWidget;

public class FormFieldComponent<W extends PWidget> implements IsPWidget {

    private final PGrid mainLayout;

    private final PHorizontalPanel headerLayout;

    private final W input;

    private final PLabel requiredLabel = new PLabel("*");

    private final PLabel captionLabel = new PLabel();

    private final PImage errorImage = new PImage("images/error.png");

    public FormFieldComponent(final W w) {
        mainLayout = new PGrid(2, 1);
        mainLayout.setWidth("100%");
        mainLayout.setCellPadding(0);
        mainLayout.setCellSpacing(0);
        mainLayout.getColumnFormatter().setWidth(0, "100%");

        headerLayout = new PHorizontalPanel();
        errorImage.setStyleProperty("color", "red");
        requiredLabel.setVisible(false);
        errorImage.setVisible(false);
        captionLabel.setVisible(false);
        headerLayout.add(requiredLabel);
        headerLayout.add(captionLabel);
        headerLayout.add(errorImage);
        mainLayout.setWidget(0, 0, headerLayout);
        mainLayout.setWidget(1, 0, w);

        this.input = w;
    }

    public void setCaption(final String caption) {
        if (caption != null) {
            captionLabel.setText(caption);
            captionLabel.setVisible(true);
        } else {
            captionLabel.setText(null);
            captionLabel.setVisible(false);
        }
    }

    public void setRequired(final boolean required) {
        requiredLabel.setVisible(required);
    }

    public void addErrorMessage(final String message) {
        errorImage.setTitle(errorImage.getTitle() == null ? message : errorImage.getTitle() + "\n" + message);
        errorImage.setVisible(true);
    }

    public void clearErrors() {
        errorImage.setTitle(null);
        errorImage.setVisible(false);
    }

    public W getInput() {
        return input;
    }

    @Override
    public PWidget asWidget() {
        return mainLayout;
    }

}