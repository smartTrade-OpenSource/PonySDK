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

package com.ponysdk.core.ui.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.writer.ModelWriter;

/**
 * A panel that can "pop up" over other widgets. It overlays the browser's client area (and any
 * previously-created popups).
 * <p>
 * A PPopupPanel should not generally be added to other panels; rather, it should be shown and
 * hidden using the {@link #show()} and {@link #hide()} methods.
 * </p>
 * <p>
 * The width and height of the PPopupPanel cannot be explicitly set; they are determined by the
 * PPopupPanel's widget. Calls to {@link #setWidth(String)} and {@link #setHeight(String)} will call
 * these methods on the PPopupPanel's widget.
 * </p>
 * <p>
 * The PopupPanel can be optionally displayed with a "glass" element behind it, which is commonly
 * used to gray out the widgets behind it. It can be enabled using {@link #setGlassEnabled(boolean)}
 * . It has a default style name of "gwt-PopupPanelGlass", which can be changed using
 * {@link #setGlassStyleName(String)}.
 * </p>
 * <h2>CSS Style Rules</h2>
 * <dl>
 * <dt>.gwt-PopupPanel</dt>
 * <dd>the outside of the popup</dd>
 * <dt>.gwt-PopupPanel .popupContent</dt>
 * <dd>the wrapper around the content</dd>
 * <dt>.gwt-PopupPanelGlass</dt>
 * <dd>the glass background behind the popup</dd>
 * </dl>
 */
public class PPopupPanel extends PSimplePanel {

    private static final Logger log = LoggerFactory.getLogger(PPopupPanel.class);

    private final boolean autoHide;
    private final List<PCloseHandler> listeners = new ArrayList<>();

    private boolean animationEnabled;

    private PPositionCallback positionCallback;

    protected PPopupPanel(final boolean autoHide) {
        this.visible = false;
        this.autoHide = autoHide;
    }

    protected PPopupPanel() {
        this(false);
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        if (autoHide) writer.write(ServerToClientModel.POPUP_AUTO_HIDE, autoHide);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.POPUP_PANEL;
    }

    public void setModal(final boolean modal) {
        saveUpdate(ServerToClientModel.MODAL, modal);
    }

    public void setDraggable(final boolean draggable) {
        saveUpdate(ServerToClientModel.DRAGGABLE, draggable);
    }

    public void center() {
        this.visible = true;
        saveUpdate(writer -> writer.write(ServerToClientModel.CENTER));
    }

    public void show() {
        if (!visible) {
            visible = true;
            saveUpdate(writer -> writer.write(ServerToClientModel.OPEN));
        } else {
            log.warn("The popup is already opened : {}", this);
        }
    }

    public void hide() {
        if (visible) {
            visible = false;
            saveUpdate(writer -> writer.write(ServerToClientModel.CLOSE));
        } else {
            log.warn("The popup is already hidden : {}", this);
        }
    }

    public void close() {
        hide();
        removeFromParent();
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public void setAnimationEnabled(final boolean animationEnabled) {
        if (Objects.equals(this.animationEnabled, animationEnabled)) return;
        this.animationEnabled = animationEnabled;
        saveUpdate(ServerToClientModel.ANIMATION, animationEnabled);
    }

    public void setGlassEnabled(final boolean glassEnabled) {
        saveUpdate(ServerToClientModel.POPUP_GLASS_ENABLED, glassEnabled);
    }

    /**
     * @deprecated Use {@link #isVisible()} directly
     * @since v2.7.8
     */
    @Deprecated
    public boolean isShowing() {
        return isVisible();
    }

    @Override
    public void setVisible(final boolean visible) {
        if (visible) show();
        else hide();
    }

    public void setPopupPosition(final int left, final int top) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.POSITION_LEFT, left);
            writer.write(ServerToClientModel.POSITION_TOP, top);
        });
    }

    public void setPopupPositionAndShow(final PPositionCallback callback) {
        this.positionCallback = callback;
        this.visible = true;
        saveAddHandler(HandlerModel.HANDLER_POPUP_POSITION);
    }

    public void addCloseHandler(final PCloseHandler handler) {
        listeners.add(handler);
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (!isVisible()) return;
        if (instruction.containsKey(ClientToServerModel.POPUP_POSITION.toStringValue())) {
            final JsonArray widgetInfo = instruction.getJsonArray(ClientToServerModel.POPUP_POSITION.toStringValue());
            int i = 0;
            final int windowWidth = ((JsonNumber) widgetInfo.get(i++)).intValue();
            final int windowHeight = ((JsonNumber) widgetInfo.get(i++)).intValue();
            final int clientWith = ((JsonNumber) widgetInfo.get(i++)).intValue();
            final int clientHeight = ((JsonNumber) widgetInfo.get(i++)).intValue();

            setPosition(windowWidth, windowHeight, clientWith, clientHeight);

            saveUpdate(writer -> writer.write(ServerToClientModel.POPUP_POSITION_AND_SHOW));
        } else if (instruction.containsKey(ClientToServerModel.HANDLER_CLOSE.toStringValue())) {
            this.visible = false;
            listeners.forEach(handler -> handler.onClose(new PCloseEvent(this)));
        } else {
            super.onClientData(instruction);
        }
    }

    public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
        this.positionCallback.setPosition(offsetWidth, offsetHeight, windowWidth, windowHeight);
        this.visible = false;
        setVisible(true);
    }

    public void setGlassStyleName(final String glassStyleName) {
        saveUpdate(ServerToClientModel.POPUP_GLASS_STYLE_NAME, glassStyleName);
    }

    /**
     * A callback that is used to set the position of a {@link PPopupPanel}
     * right before it is shown.
     */
    public interface PPositionCallback {

        void setPosition(int offsetWidth, int offsetHeight, int windowWidth, int windowHeight);
    }
}
