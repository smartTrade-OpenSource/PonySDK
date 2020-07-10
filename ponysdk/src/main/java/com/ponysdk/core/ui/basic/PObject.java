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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.PTerminalEvent;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.util.SetUtils;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.core.writer.ModelWriterCallback;

/**
 * The superclass for all PonySDK objects.
 */
public abstract class PObject {

    protected final int ID = UIContext.get().nextID();
    protected PWindow window;
    protected PFrame frame;
    protected Set<InitializeListener> initializeListeners;
    protected Set<DestroyListener> destroyListeners;
    protected Object data;

    protected LinkedHashMap<Integer, Runnable> stackedInstructions;
    private String nativeBindingFunction;

    private PTerminalEvent.Handler terminalHandler;

    protected boolean initialized = false;
    protected boolean destroy = false;

    protected int saveKey = ServerToClientModel.MAX_VALUE; // Has to be higher than all ordinal of ServerToClientModel
    private AjaxHandler ajaxHandler;

    PObject() {
    }

    /**
     * Gets the widget type
     *
     * @return the widget type
     */
    protected abstract WidgetType getWidgetType();

    protected boolean attach(final PWindow window, final PFrame frame) {
        this.frame = frame;

        if (this.window == null && window != null) {
            this.window = window;
            init();
            return true;
        } else if (this.window != window) {
            throw new IllegalAccessError(
                "Widget already attached to an other window, current window : #" + this.window + ", new window : #" + window);
        }

        return false;
    }

    void init() {
        if (initialized) return;
        if (isParentInitialized()) applyInit();
        else if (frame != null && !frame.isInitialized()) frame.addInitializeListener(event -> applyInit());
        else window.addOpenHandler(window -> applyInit());
    }

    public boolean isParentInitialized() {
        return window.isOpened() && (frame == null || frame.isInitialized());
    }

    protected void applyInit() {
        final ModelWriter writer = UIContext.get().getWriter();
        writer.beginObject(window);
        if (frame != null) writer.write(ServerToClientModel.FRAME_ID, frame.getID());
        writer.write(ServerToClientModel.TYPE_CREATE, ID);
        writer.write(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
        enrichForCreation(writer);
        enrichForUpdate(writer);
        writer.endObject();

        UIContext.get().registerObject(this);

        init0();

        if (stackedInstructions != null) {
            stackedInstructions.values().forEach(Runnable::run);
            stackedInstructions = null;
        }

        initialized = true;

        if (initializeListeners != null) initializeListeners.forEach(listener -> listener.onInitialize(this));
    }

    /**
     * Enrichs on the initialization for the creation
     *
     * @param writer the writer
     */
    protected void enrichForCreation(final ModelWriter writer) {
    }

    /**
     * Enrichs on the initialization for the update
     *
     * @param writer the writer
     */
    protected void enrichForUpdate(final ModelWriter writer) {
    }

    void init0() {
    }

    public PWindow getWindow() {
        return window;
    }

    public PFrame getFrame() {
        return frame;
    }

    public final int getID() {
        return ID;
    }

    /**
     * Bind to a Terminal function, usefull to link the objectID and the widget
     * reference
     * <p>
     * <h2>Example :</h2>
     * <p>
     *
     * <pre>
     * --- Java ---
     *
     * bindTerminalFunction("myFunction")
     * </pre>
     * <p>
     *
     * <pre>
     * --- JavaScript ---
     *
     * myFunction(id, object) {
     * ....
     * ....
     * }
     * </pre>
     */
    public void bindTerminalFunction(final String functionName) {
        if (nativeBindingFunction != null)
            throw new IllegalAccessError("Object already bind to native function: " + nativeBindingFunction);

        nativeBindingFunction = functionName;

        saveUpdate(writer -> writer.write(ServerToClientModel.BIND, functionName));
    }

    public void sendToNative(final JsonObject data) {
        if (destroy) return;
        if (nativeBindingFunction == null) throw new IllegalAccessError("Object not bind to a native function");

        saveUpdate(writer -> writer.write(ServerToClientModel.NATIVE, data.toString()));
    }

    public void setTerminalHandler(final PTerminalEvent.Handler terminalHandler) {
        this.terminalHandler = terminalHandler;
    }

    /**
     * JSON received from the Terminal using pony.sendDataToServer(objectID, JSON)
     */
    public void onClientData(final JsonObject event) {
        if (destroy) return;
        if (terminalHandler != null) {
            final String nativeKey = ClientToServerModel.NATIVE.toStringValue();
            if (event.containsKey(nativeKey)) {
                terminalHandler.onTerminalEvent(new PTerminalEvent(this, event.getJsonObject(nativeKey)));
            }
        }
    }

    protected LinkedHashMap<Integer, Runnable> safeStackedInstructions() {
        if (stackedInstructions == null) stackedInstructions = new LinkedHashMap<>(8);
        return stackedInstructions;
    }

    protected void saveUpdate(final ModelWriterCallback callback) {
        saveUpdate(saveKey++, callback);
    }

    protected void saveUpdate(final ServerToClientModel serverToClientModel, final Object value) {
        saveUpdate(serverToClientModel.getValue(), writer -> writer.write(serverToClientModel, value));
    }

    private void saveUpdate(final int atomicKey, final ModelWriterCallback callback) {
        if (destroy) return;

        if (initialized) writeUpdate(callback);
        else safeStackedInstructions().put(atomicKey, () -> writeUpdate(callback));
    }

    void writeUpdate(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = UIContext.get().getWriter();
        writer.beginObject(window);
        if (frame != null) writer.write(ServerToClientModel.FRAME_ID, frame.getID());
        writer.write(ServerToClientModel.TYPE_UPDATE, ID);

        callback.doWrite(writer);
        writer.endObject();
    }

    protected void saveAdd(final int objectID, final int parentObjectID) {
        saveAdd(objectID, parentObjectID, (ServerBinaryModel) null);
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final ServerBinaryModel... binaryModels) {
        if (destroy) return;

        final ModelWriterCallback callback = writer -> {
            writer.write(ServerToClientModel.TYPE_ADD, objectID);
            writer.write(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
            if (binaryModels != null) {
                for (final ServerBinaryModel binaryModel : binaryModels) {
                    if (binaryModel != null) writer.write(binaryModel.getKey(), binaryModel.getValue());
                }
            }
        };
        if (initialized) writeAdd(callback);
        else safeStackedInstructions().put(saveKey++, () -> writeAdd(callback));
    }

    private void writeAdd(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = UIContext.get().getWriter();
        writer.beginObject(window);
        if (frame != null) writer.write(ServerToClientModel.FRAME_ID, frame.getID());

        callback.doWrite(writer);
        writer.endObject();
    }

    protected void saveAddHandler(final HandlerModel type) {
        if (destroy) return;

        final ModelWriterCallback callback = writer -> writer.write(ServerToClientModel.HANDLER_TYPE, type.getValue());
        if (initialized) writeAddHandler(callback);
        else safeStackedInstructions().put(saveKey++, () -> writeAddHandler(callback));
    }

    void writeAddHandler(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = UIContext.get().getWriter();
        writer.beginObject(window);
        if (frame != null) writer.write(ServerToClientModel.FRAME_ID, frame.getID());
        writer.write(ServerToClientModel.TYPE_ADD_HANDLER, ID);

        callback.doWrite(writer);
        writer.endObject();
    }

    protected void saveRemoveHandler(final HandlerModel type) {
        if (destroy) return;

        final ModelWriterCallback callback = writer -> {
            writer.write(ServerToClientModel.TYPE_REMOVE_HANDLER, ID);
            writer.write(ServerToClientModel.HANDLER_TYPE, type.getValue());
        };
        if (initialized) writeRemoveHandler(callback);
        else safeStackedInstructions().put(saveKey++, () -> writeRemoveHandler(callback));
    }

    private void writeRemoveHandler(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = UIContext.get().getWriter();
        writer.beginObject(window);
        if (frame != null) writer.write(ServerToClientModel.FRAME_ID, frame.getID());

        callback.doWrite(writer);
        writer.endObject();
    }

    void saveRemove(final int objectID, final int parentObjectID) {
        if (destroy) return;

        final ModelWriterCallback callback = writer -> {
            writer.write(ServerToClientModel.TYPE_REMOVE, objectID);
            writer.write(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        };
        if (initialized) writeRemove(callback);
        else safeStackedInstructions().put(saveKey++, () -> writeRemove(callback));
    }

    private void writeRemove(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = UIContext.get().getWriter();
        writer.beginObject(window);
        if (frame != null) writer.write(ServerToClientModel.FRAME_ID, frame.getID());

        callback.doWrite(writer);
        writer.endObject();
    }

    public void addInitializeListener(final InitializeListener listener) {
        if (this.initializeListeners == null) initializeListeners = SetUtils.newArraySet(4);
        this.initializeListeners.add(listener);
    }

    public void addDestroyListener(final DestroyListener listener) {
        if (this.destroyListeners == null) destroyListeners = SetUtils.newArraySet(4);
        this.destroyListeners.add(listener);
    }

    public void onDestroy() {
        destroy = true;
        terminalHandler = null;
        initializeListeners = null;
        if (this.destroyListeners != null) this.destroyListeners.forEach(listener -> listener.onDestroy(this));
        this.destroyListeners = null;
        window = null;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final PObject other = (PObject) obj;
        return ID == other.ID;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + ID;
    }

    /**
     * Usage : Add a HTTP Request Handler and it will be awaken by an ajax request
     * Sample in JQuery : $.get({url : pony.getHostPageBaseURL() + "/ajax", headers : {
     * "UI_CONTEXT_ID" : pony.getContextId(), "OBJECT_ID" : this.id } });
     */
    public void setAjaxHandler(final AjaxHandler httpRequestHandler) {
        this.ajaxHandler = httpRequestHandler;
    }

    public final void handleAjaxRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (ajaxHandler != null) ajaxHandler.handleAjaxRequest(request, response);
    }

    @FunctionalInterface
    public static interface AjaxHandler {

        void handleAjaxRequest(final HttpServletRequest request, final HttpServletResponse response)
                throws ServletException, IOException;
    }

    @FunctionalInterface
    public interface InitializeListener {

        void onInitialize(PObject object);
    }

    @FunctionalInterface
    public interface DestroyListener {

        void onDestroy(PObject object);
    }

}
