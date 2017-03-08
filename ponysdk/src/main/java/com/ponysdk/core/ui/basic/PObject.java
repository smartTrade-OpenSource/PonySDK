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

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.WebsocketEncoder;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.event.PDestroyEvent;
import com.ponysdk.core.ui.basic.event.PTerminalEvent;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.core.writer.ModelWriterCallback;

/**
 * The superclass for all PonySDK objects.
 */
public abstract class PObject implements PDestroyEvent.HasHandler {

    protected final int ID = UIContext.get().nextID();
    protected PWindow window;
    protected AttachListener attachListener;
    protected Object data;

    boolean initialized = false;
    protected Queue<Runnable> stackedInstructions;

    private String nativeBindingFunction;
    private PTerminalEvent.Handler terminalHandler;
    private Set<PDestroyEvent.Handler> destroyHandlers;

    protected boolean destroy = false;

    PObject() {
    }

    protected abstract WidgetType getWidgetType();

    protected boolean attach(final PWindow window) {
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
        if (window.isOpened()) {
            applyInit();
        } else {
            window.addOpenHandler(window -> applyInit());
        }
    }

    private void applyInit() {
        final WebsocketEncoder parser = Txn.get().getEncoder();
        parser.beginObject();
        if (window != PWindow.getMain()) parser.encode(ServerToClientModel.WINDOW_ID, window.getID());
        parser.encode(ServerToClientModel.TYPE_CREATE, ID);
        parser.encode(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
        enrichOnInit(parser);
        parser.endObject();

        UIContext.get().registerObject(this);

        init0();

        if (stackedInstructions != null) {
            while (!stackedInstructions.isEmpty()) {
                stackedInstructions.poll().run();
            }
        }

        if (attachListener != null) attachListener.onAttach();

        initialized = true;
    }

    protected void enrichOnInit(final WebsocketEncoder encoder) {
    }

    protected void init0() {
    }

    public PWindow getWindow() {
        return window;
    }

    public final int getID() {
        return ID;
    }

    /**
     * Bind to a Terminal function, usefull to link the objectID and the widget reference
     *
     * <h2>Example :</h2>
     *
     * <pre>
     * --- Java ---
     *
     * bindTerminalFunction("myFunction")
     * </pre>
     *
     * <pre>
     * --- JavaScript ---
     *
     * myFunction(id, object) {
     * ....
     * ....
     * }
     * </pre>
     *
     * @param functionName
     */
    public void bindTerminalFunction(final String functionName) {
        if (nativeBindingFunction != null)
            throw new IllegalAccessError("Object already bind to native function: " + nativeBindingFunction);

        nativeBindingFunction = functionName;

        saveUpdate(writer -> writer.writeModel(ServerToClientModel.BIND, functionName));
    }

    public void sendToNative(final JsonObject data) {
        if (destroy) return;
        if (nativeBindingFunction == null) throw new IllegalAccessError("Object not bind to a native function");

        saveUpdate(writer -> writer.writeModel(ServerToClientModel.NATIVE, data));
    }

    public void setTerminalHandler(final PTerminalEvent.Handler terminalHandler) {
        this.terminalHandler = terminalHandler;
    }

    /**
     * JSON received from the Terminal using pony.sendDataToServer(objectID, JSON)
     *
     * @param event
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

    protected Queue<Runnable> safeStackedInstructions() {
        if (stackedInstructions == null) stackedInstructions = new LinkedList<>();
        return stackedInstructions;
    }

    protected void saveAdd(final int objectID, final int parentObjectID) {
        saveAdd(objectID, parentObjectID, (ServerBinaryModel) null);
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final ServerBinaryModel... binaryModels) {
        if (destroy) return;
        if (initialized) executeAdd(objectID, parentObjectID, binaryModels);
        else safeStackedInstructions().add(() -> executeAdd(objectID, parentObjectID, binaryModels));
    }

    private void executeAdd(final int objectID, final int parentObjectID, final ServerBinaryModel... binaryModels) {
        if (destroy) return;
        final WebsocketEncoder encoder = Txn.get().getEncoder();
        encoder.beginObject();
        if (!PWindow.isMain(window)) encoder.encode(ServerToClientModel.WINDOW_ID, window.getID());
        encoder.encode(ServerToClientModel.TYPE_ADD, objectID);
        encoder.encode(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        if (binaryModels != null) {
            for (final ServerBinaryModel binaryModel : binaryModels) {
                if (binaryModel != null) encoder.encode(binaryModel.getKey(), binaryModel.getValue());
            }
        }
        encoder.endObject();
    }

    protected void saveAddHandler(final HandlerModel type) {
        if (destroy) return;
        if (initialized) executeAddHandler(type);
        else safeStackedInstructions().add(() -> executeAddHandler(type));
    }

    private void executeAddHandler(final HandlerModel type) {
        if (destroy) return;
        final WebsocketEncoder parser = Txn.get().getEncoder();
        parser.beginObject();
        if (!PWindow.isMain(window)) parser.encode(ServerToClientModel.WINDOW_ID, window.getID());
        parser.encode(ServerToClientModel.TYPE_ADD_HANDLER, type.getValue());
        parser.encode(ServerToClientModel.OBJECT_ID, ID);
        parser.endObject();
    }

    protected void saveRemoveHandler(final HandlerModel type) {
        if (destroy) return;
        if (initialized) executeRemoveHandler();
        else safeStackedInstructions().add(this::executeRemoveHandler);
    }

    private void executeRemoveHandler() {
        if (destroy) return;
        final WebsocketEncoder parser = Txn.get().getEncoder();
        parser.beginObject();
        if (!PWindow.isMain(window)) parser.encode(ServerToClientModel.WINDOW_ID, window.getID());
        parser.encode(ServerToClientModel.TYPE_REMOVE_HANDLER, ID);
        parser.endObject();
    }

    void saveRemove(final int objectID, final int parentObjectID) {
        if (destroy) return;
        if (initialized) executeRemove(objectID, parentObjectID);
        else safeStackedInstructions().add(() -> executeRemove(objectID, parentObjectID));
    }

    private void executeRemove(final int objectID, final int parentObjectID) {
        if (destroy) return;
        final WebsocketEncoder parser = Txn.get().getEncoder();
        parser.beginObject();
        if (!PWindow.isMain(window)) parser.encode(ServerToClientModel.WINDOW_ID, window.getID());
        parser.encode(ServerToClientModel.TYPE_REMOVE, objectID);
        parser.encode(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        parser.endObject();
    }

    protected void saveUpdate(final ModelWriterCallback callback) {
        if (destroy) return;
        if (initialized) writeUpdate(callback);
        else safeStackedInstructions().add(() -> writeUpdate(callback));
    }

    void writeUpdate(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = Txn.getWriter();
        writer.beginObject();
        if (!PWindow.isMain(window)) writer.writeModel(ServerToClientModel.WINDOW_ID, window.getID());
        writer.writeModel(ServerToClientModel.TYPE_UPDATE, ID);

        callback.doWrite(writer);
        writer.endObject();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + ID;
    }

    public void setAttachListener(final AttachListener attachListener) {
        this.attachListener = attachListener;
    }

    @Override
    public void addDestroyHandler(final PDestroyEvent.Handler handler) {
        if (destroyHandlers == null) destroyHandlers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        destroyHandlers.add(handler);
    }

    @Override
    public boolean removeDestroyHandler(final PDestroyEvent.Handler handler) {
        return destroyHandlers != null && destroyHandlers.remove(handler);
    }

    public void destroy() {
        destroy = true;
        terminalHandler = null;
        attachListener = null;
        if (destroyHandlers != null) {
            final PDestroyEvent.Event destroyEvent = new PDestroyEvent.Event(this);
            destroyHandlers.forEach(handler -> handler.onDestroy(destroyEvent));
            destroyHandlers = null;
        }
    }

    public void setData(final Object data) {
        this.data = data;
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

    @FunctionalInterface
    public interface AttachListener {

        void onAttach();
    }

    public boolean isInitialized() {
        return initialized;
    }

}
