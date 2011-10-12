/*
 * Copyright (c) 2011 PonySDK
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
package com.ponysdk.ui.server.basic;

import com.ponysdk.core.PonySession;
import com.ponysdk.ui.server.basic.event.HasPAnimation;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PPopupPanel extends PSimplePanel implements HasPAnimation, PPositionCallback {

    private final boolean autoHide;
    private boolean glassEnabled;
    private boolean animationEnabled;
    private String glassStyleName;
    private boolean center;
    private boolean showing;
    private PPositionCallback positionCallback;
    private int leftPosition;
    private int topPosition;

    public PPopupPanel() {
        this(false);
    }

    public PPopupPanel(final boolean autoHide) {
        super();
        this.autoHide = autoHide;

        removeFromParent();
        final PWidgetCollection children = PonySession.getCurrent().getRootLayoutPanel().getChildren();
        children.insert(this, children.size());
        PonySession.getCurrent().getRootLayoutPanel().adopt(this);

        final Property mainProperty = new Property();
        mainProperty.setProperty(PropertyKey.POPUP_AUTO_HIDE, autoHide);
        setMainProperty(mainProperty);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.POPUP_PANEL;
    }

    public void setGlassEnabled(boolean glassEnabled) {
        this.glassEnabled = glassEnabled;
        final Update update = new Update(ID);
        update.setMainPropertyValue(PropertyKey.POPUP_GLASS_ENABLED, glassEnabled);
        getPonySession().stackInstruction(update);
    }

    @Override
    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
        final Update update = new Update(ID);
        update.setMainPropertyValue(PropertyKey.ANIMATION, animationEnabled);
        getPonySession().stackInstruction(update);
    }

    public void center() {
        this.center = true;
        final Update update = new Update(ID);
        update.setMainPropertyKey(PropertyKey.POPUP_CENTER);
        getPonySession().stackInstruction(update);
    }

    public void show() {
        if (!showing) {
            this.showing = true;
            final Update update = new Update(ID);
            update.setMainPropertyKey(PropertyKey.POPUP_SHOW);
            getPonySession().stackInstruction(update);
        }
    }

    public void hide() {
        if (showing) {
            this.showing = false;
            final Update update = new Update(ID);
            update.setMainPropertyKey(PropertyKey.POPUP_HIDE);
            getPonySession().stackInstruction(update);
        }
    }

    public void setGlassStyleName(String glassStyleName) {
        this.glassStyleName = glassStyleName;
        final Update update = new Update(ID);
        update.setMainPropertyValue(PropertyKey.POPUP_GLASS_STYLE_NAME, glassStyleName);
        getPonySession().stackInstruction(update);
    }

    @Override
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public boolean isGlassEnabled() {
        return glassEnabled;
    }

    public boolean isShowing() {
        return showing;
    }

    public void setPopupPosition(int left, int top) {
        leftPosition = left;
        topPosition = top;

        final Property property = new Property(PropertyKey.POPUP_POSITION);
        property.setProperty(PropertyKey.POPUP_POSITION_LEFT, leftPosition);
        property.setProperty(PropertyKey.POPUP_POSITION_TOP, topPosition);

        final Update updateLeft = new Update(ID);
        updateLeft.setMainProperty(property);
        getPonySession().stackInstruction(updateLeft);
    }

    public void setPopupPositionAndShow(PPositionCallback callback) {
        this.positionCallback = callback;
        this.showing = true;
        final AddHandler handler = new AddHandler(ID, HandlerType.POPUP_POSITION_CALLBACK); // remove ordinal ?
        getPonySession().stackInstruction(handler);
    }

    @Override
    public void onEventInstruction(EventInstruction instruction) {
        if (HandlerType.POPUP_POSITION_CALLBACK.equals(instruction.getHandlerType())) {
            final Integer windowWidth = instruction.getMainProperty().getIntProperty(PropertyKey.OFFSETWIDTH);
            final Integer windowHeight = instruction.getMainProperty().getIntProperty(PropertyKey.OFFSETHEIGHT);
            final Integer clientWith = instruction.getMainProperty().getIntProperty(PropertyKey.CLIENT_WIDTH);
            final Integer clientHeight = instruction.getMainProperty().getIntProperty(PropertyKey.CLIENT_HEIGHT);
            setPosition(windowWidth, windowHeight, clientWith, clientHeight);
        } else {
            super.onEventInstruction(instruction);
        }
    }

    @Override
    public void setPosition(int offsetWidth, int offsetHeight, int windowWidth, int windowHeight) {
        this.positionCallback.setPosition(offsetWidth, offsetHeight, windowWidth, windowHeight);
        setVisible(true);
    }

    public boolean isAutoHide() {
        return autoHide;
    }

    public String getGlassStyleName() {
        return glassStyleName;
    }

    public boolean isCenter() {
        return center;
    }

    public PPositionCallback getPositionCallback() {
        return positionCallback;
    }

    public int getLeftPosition() {
        return leftPosition;
    }
}
