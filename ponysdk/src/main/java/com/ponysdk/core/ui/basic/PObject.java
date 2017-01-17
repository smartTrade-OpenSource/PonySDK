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

import java.util.LinkedList;
import java.util.Queue;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.event.PTerminalEvent;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.core.writer.ModelWriterCallback;

/**
 * The superclass for all PonySDK objects.
 */
public abstract class PObject {

    private static final Logger log = LoggerFactory.getLogger(PObject.class);

    protected final int ID = UIContext.get().nextID();
    final Queue<Runnable> stackedInstructions = new LinkedList<>();
    protected int windowID = PWindow.EMPTY_WINDOW_ID;
    boolean initialized = false;
    private String nativeBindingFunction;
    private PTerminalEvent.Handler terminalHandler;
    private AttachListener attachListener;

    PObject() {
        UIContext.get().registerObject(this);
    }

    protected boolean attach(final int windowID) {
        if (this.windowID == PWindow.EMPTY_WINDOW_ID && windowID != PWindow.EMPTY_WINDOW_ID) {
            this.windowID = windowID;
            init();
            return true;
        } else if (this.windowID != windowID) {
            throw new IllegalAccessError(
                "Widget already attached to an other window, current window : #" + this.windowID + ", new window : #" + windowID);
        }

        return false;
    }

    void init() {
        if (initialized) return;
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (windowID != PWindow.getMain().getID()) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
        parser.parse(ServerToClientModel.TYPE_CREATE, ID);
        parser.parse(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
        enrichOnInit(parser);
        parser.endObject();

        init0();

        while (!stackedInstructions.isEmpty())
            stackedInstructions.poll().run();

        if (attachListener != null) attachListener.onAttach();

        initialized = true;
    }

    protected void enrichOnInit(final Parser parser) {
    }

    protected void init0() {
    }

    public int getWindowID() {
        return windowID;
    }

    public PWindow getWindow() {
        return PWindowManager.getWindow(windowID);
    }

    protected abstract WidgetType getWidgetType();

    public final int getID() {
        return ID;
    }

    public void bindTerminalFunction(final String functionName) {
        if (nativeBindingFunction != null)
            throw new IllegalAccessError("Object already bind to native function: " + nativeBindingFunction);

        nativeBindingFunction = functionName;

        saveUpdate(writer -> writer.writeModel(ServerToClientModel.BIND, functionName));
    }

    public void sendToNative(final JsonObject data) {
        if (nativeBindingFunction == null) throw new IllegalAccessError("Object not bind to a native function");

        saveUpdate(writer -> writer.writeModel(ServerToClientModel.NATIVE, data));
    }

    public void setTerminalHandler(final PTerminalEvent.Handler terminalHandler) {
        this.terminalHandler = terminalHandler;
    }

    public void onClientData(final JsonObject event) {
        if (terminalHandler != null) {
            final String nativeKey = ClientToServerModel.NATIVE.toStringValue();
            if (event.containsKey(nativeKey)) {
                terminalHandler.onTerminalEvent(new PTerminalEvent(this, event.getJsonObject(nativeKey)));
            }
        }
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

    protected void saveAdd(final int objectID, final int parentObjectID) {
        saveAdd(objectID, parentObjectID, (ServerBinaryModel) null);
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final ServerBinaryModel... binaryModels) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) executeAdd(objectID, parentObjectID, binaryModels);
        else stackedInstructions.add(() -> executeAdd(objectID, parentObjectID, binaryModels));
    }

    private void executeAdd(final int objectID, final int parentObjectID, final ServerBinaryModel... binaryModels) {
        if (PWindowManager.getWindow(windowID) != null) {
            final Parser parser = Txn.get().getParser();
            parser.beginObject();
            if (windowID != PWindow.getMain().getID()) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
            parser.parse(ServerToClientModel.TYPE_ADD, objectID);
            parser.parse(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
            if (binaryModels != null) {
                for (final ServerBinaryModel binaryModel : binaryModels) {
                    if (binaryModel != null) parser.parse(binaryModel.getKey(), binaryModel.getValue());
                }
            }
            parser.endObject();
        } else {
            if (log.isWarnEnabled()) log.warn("Add : The attached window #" + windowID + " doesn't exist");
        }
    }

    protected void saveAddHandler(final HandlerModel type) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) executeAddHandler(type);
        else stackedInstructions.add(() -> executeAddHandler(type));
    }

    private void executeAddHandler(final HandlerModel type) {
        if (PWindowManager.getWindow(windowID) != null) {
            final Parser parser = Txn.get().getParser();
            parser.beginObject();
            if (windowID != PWindow.getMain().getID()) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
            parser.parse(ServerToClientModel.TYPE_ADD_HANDLER, type.getValue());
            parser.parse(ServerToClientModel.OBJECT_ID, ID);
            parser.endObject();
        } else {
            if (log.isWarnEnabled()) log.warn("AddHandler : The attached window #" + windowID + " doesn't exist");
        }
    }

    protected void saveRemoveHandler(final HandlerModel type) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) executeRemoveHandler();
        else stackedInstructions.add(this::executeRemoveHandler);
    }

    private void executeRemoveHandler() {
        if (PWindowManager.getWindow(windowID) != null) {
            final Parser parser = Txn.get().getParser();
            parser.beginObject();
            if (windowID != PWindow.getMain().getID()) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
            parser.parse(ServerToClientModel.TYPE_REMOVE_HANDLER, ID);
            parser.endObject();
        } else {
            if (log.isWarnEnabled()) log.warn("RemoveHandler : The attached window #" + windowID + " doesn't exist");
        }
    }

    void saveRemove(final int objectID, final int parentObjectID) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) executeRemove(objectID, parentObjectID);
        else stackedInstructions.add(() -> executeRemove(objectID, parentObjectID));
    }

    private void executeRemove(final int objectID, final int parentObjectID) {
        if (PWindowManager.getWindow(windowID) != null) {
            final Parser parser = Txn.get().getParser();
            parser.beginObject();
            if (windowID != PWindow.getMain().getID()) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
            parser.parse(ServerToClientModel.TYPE_REMOVE, objectID);
            parser.parse(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
            parser.endObject();
        } else {
            if (log.isWarnEnabled()) log.warn("Remove : The attached window #" + windowID + " doesn't exist");
        }
    }

    protected void saveUpdate(final ModelWriterCallback callback) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) writeUpdate(callback);
        else stackedInstructions.add(() -> writeUpdate(callback));
    }

    private void writeUpdate(final ModelWriterCallback callback) {
        if (PWindowManager.getWindow(windowID) != null) {
            final ModelWriter writer = Txn.getWriter();
            writer.beginObject();
            if (windowID != PWindow.getMain().getID()) {
                writer.writeModel(ServerToClientModel.WINDOW_ID, windowID);
            }
            writer.writeModel(ServerToClientModel.TYPE_UPDATE, ID);

            callback.doWrite(writer);
            writer.endObject();
        } else {
            if (log.isWarnEnabled()) log.warn("Update : The attached window #" + windowID + " doesn't exist");
        }
    }

    @Override
    public String toString() {
        return "ID=" + ID + ", widgetType=" + getWidgetType().name();
    }

    public void setAttachListener(final AttachListener attachListener) {
        this.attachListener = attachListener;
    }

    @FunctionalInterface
    interface AttachListener {

        void onAttach();
    }

}
