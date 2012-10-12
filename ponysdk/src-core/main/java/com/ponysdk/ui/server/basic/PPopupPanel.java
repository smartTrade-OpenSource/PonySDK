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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.HasPAnimation;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.PCloseHandler;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A panel that can "pop up" over other widgets. It overlays the browser's client area (and any
 * previously-created popups).
 * <p>
 * A PPopupPanel should not generally be added to other panels; rather, it should be shown and hidden using
 * the {@link #show()} and {@link #hide()} methods.
 * </p>
 * <p>
 * The width and height of the PPopupPanel cannot be explicitly set; they are determined by the PPopupPanel's
 * widget. Calls to {@link #setWidth(String)} and {@link #setHeight(String)} will call these methods on the
 * PPopupPanel's widget.
 * </p>
 * <p>
 * <img class='gallery' src='doc-files/PPopupPanel.png'/>
 * </p>
 * <p>
 * The PopupPanel can be optionally displayed with a "glass" element behind it, which is commonly used to gray
 * out the widgets behind it. It can be enabled using {@link #setGlassEnabled(boolean)}. It has a default
 * style name of "gwt-PopupPanelGlass", which can be changed using {@link #setGlassStyleName(String)}.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-PopupPanel</dt>
 * <dd>the outside of the popup</dd>
 * <dt>.gwt-PopupPanel .popupContent</dt>
 * <dd>the wrapper around the content</dd>
 * <dt>.gwt-PopupPanelGlass</dt>
 * <dd>the glass background behind the popup</dd>
 * </dl>
 */
public class PPopupPanel extends PSimplePanel implements HasPAnimation {

    /**
     * A callback that is used to set the position of a {@link PPopupPanel} right before it is shown.
     */
    public interface PPositionCallback {

        void setPosition(int offsetWidth, int offsetHeight, int windowWidth, int windowHeight);
    }

    private final boolean autoHide;

    private boolean glassEnabled;

    private boolean animationEnabled;

    private boolean center;

    private boolean showing;

    private int leftPosition;

    private int topPosition;

    private String glassStyleName;

    private PPositionCallback positionCallback;

    private final List<PCloseHandler> listeners = new ArrayList<PCloseHandler>();

    public PPopupPanel() {
        this(false);
    }

    public PPopupPanel(final boolean autoHide) {
        super();
        this.autoHide = autoHide;

        removeFromParent();

        final PRootPanel root = PRootPanel.get();
        final PWidgetCollection children = root.getChildren();
        children.insert(this, children.size());
        root.adopt(this);

        create.put(PROPERTY.POPUP_AUTO_HIDE, autoHide);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.POPUP_PANEL;
    }

    public void setModal(final boolean modal) {
        final Update update = new Update(ID);
        update.put(PROPERTY.POPUP_MODAL, modal);
        getUIContext().stackInstruction(update);
    }

    public void setGlassEnabled(final boolean glassEnabled) {
        this.glassEnabled = glassEnabled;
        final Update update = new Update(ID);
        update.put(PROPERTY.POPUP_GLASS_ENABLED, glassEnabled);
        getUIContext().stackInstruction(update);
    }

    public void setDraggable(final boolean draggable) {
        if (draggable) {
            final Update update = new Update(ID);
            update.put(PROPERTY.POPUP_DRAGGABLE, true);
            getUIContext().stackInstruction(update);
        }
    }

    @Override
    public void setAnimationEnabled(final boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
        final Update update = new Update(ID);
        update.put(PROPERTY.ANIMATION, animationEnabled);
        getUIContext().stackInstruction(update);
    }

    public void center() {
        this.center = true;
        this.showing = true;

        final Update update = new Update(ID);
        update.put(PROPERTY.POPUP_CENTER, center);
        getUIContext().stackInstruction(update);
    }

    public void show() {
        if (!showing) {
            this.showing = true;
            final Update update = new Update(ID);
            update.put(PROPERTY.POPUP_SHOW, showing);
            getUIContext().stackInstruction(update);
        }
    }

    public void hide() {
        if (showing) {
            this.showing = false;
            final Update update = new Update(ID);
            update.put(PROPERTY.POPUP_HIDE, showing);
            getUIContext().stackInstruction(update);
        }
    }

    public void setGlassStyleName(final String glassStyleName) {
        this.glassStyleName = glassStyleName;
        final Update update = new Update(ID);
        update.put(PROPERTY.POPUP_GLASS_STYLE_NAME, glassStyleName);
        getUIContext().stackInstruction(update);
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

    public void setPopupPosition(final int left, final int top) {
        leftPosition = left;
        topPosition = top;

        final Update update = new Update(ID);
        update.put(PROPERTY.POPUP_POSITION);
        update.put(PROPERTY.POPUP_POSITION_LEFT, leftPosition);
        update.put(PROPERTY.POPUP_POSITION_TOP, topPosition);

        getUIContext().stackInstruction(update);
    }

    public void setPopupPositionAndShow(final PPositionCallback callback) {
        this.positionCallback = callback;
        this.showing = true;

        final AddHandler handler = new AddHandler(ID, HANDLER.KEY_.POPUP_POSITION_CALLBACK);
        getUIContext().stackInstruction(handler);
    }

    public void addCloseHandler(final PCloseHandler handler) {
        listeners.add(handler);
    }

    @Override
    public void onClientData(final JSONObject instruction) throws JSONException {
        if (instruction.getString(HANDLER.KEY).equals(HANDLER.KEY_.POPUP_POSITION_CALLBACK)) {
            final Integer windowWidth = instruction.getInt(PROPERTY.OFFSETWIDTH);
            final Integer windowHeight = instruction.getInt(PROPERTY.OFFSETHEIGHT);
            final Integer clientWith = instruction.getInt(PROPERTY.CLIENT_WIDTH);
            final Integer clientHeight = instruction.getInt(PROPERTY.CLIENT_HEIGHT);
            setPosition(windowWidth, windowHeight, clientWith, clientHeight);
        } else if (HANDLER.KEY_.CLOSE_HANDLER.equals(instruction.getString(HANDLER.KEY))) {
            this.showing = false;
            fireOnClose();
        } else {
            super.onClientData(instruction);
        }
    }

    private void fireOnClose() {
        for (final PCloseHandler handler : listeners) {
            handler.onClose(new PCloseEvent(this));
        }
    }

    public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
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
