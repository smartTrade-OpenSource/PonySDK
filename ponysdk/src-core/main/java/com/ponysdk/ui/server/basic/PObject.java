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

package com.ponysdk.ui.server.basic;

import java.util.LinkedList;
import java.util.Queue;

import javax.json.JsonObject;

import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.server.model.ServerBinaryModel;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * The superclass for all PonySDK objects.
 */
public abstract class PObject {

    protected final int ID = UIContext.get().nextID();

    private String nativeBindingFunction;

    private ListenerCollection<PNativeHandler> nativeHandlers;

    protected int windowID;

    private boolean initialized = false;

    protected final Queue<Runnable> stackedInstructions = new LinkedList<>();

    PObject() {
        this(PWindow.EMPTY_WINDOW_ID);
    }

    PObject(final int windowID) {
        UIContext.get().registerObject(this);
        this.windowID = windowID;
    }

    protected boolean attach(final int windowID) {
        if (this.windowID == PWindow.EMPTY_WINDOW_ID && windowID != PWindow.EMPTY_WINDOW_ID) {
            this.windowID = windowID;
            init();

            return true;
        } else if (this.windowID != windowID) {
            throw new IllegalAccessError("Widget already attached to an other window");
        }
        return false;
    }

    protected void init() {
        if (!initialized) {
            final Parser parser = Txn.get().getParser();
            parser.beginObject();
            if (windowID != PWindow.MAIN_WINDOW_ID) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
            parser.parse(ServerToClientModel.TYPE_CREATE, ID);
            parser.parse(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
            enrichOnInit(parser);
            parser.endObject();

            init0();

            while (!stackedInstructions.isEmpty())
                stackedInstructions.poll().run();

            initialized = true;
        }
    }

    protected void init0() {
    }

    public int getWindowID() {
        return windowID;
    }

    protected void enrichOnInit(final Parser parser) {
    }

    protected abstract WidgetType getWidgetType();

    public final int getID() {
        return ID;
    }

    public void bindTerminalFunction(final String functionName) {
        if (nativeBindingFunction != null)
            throw new IllegalAccessError("Object already bind to native function: " + nativeBindingFunction);

        nativeBindingFunction = functionName;

        saveUpdate(ServerToClientModel.BIND, functionName);
    }

    public void sendToNative(final JsonObject data) {
        if (nativeBindingFunction == null)
            throw new IllegalAccessError("Object not bind to a native function");

        saveUpdate(ServerToClientModel.NATIVE, data);
    }

    public void addNativeHandler(final PNativeHandler handler) {
        if (nativeHandlers == null)
            nativeHandlers = new ListenerCollection<>();

        nativeHandlers.register(handler);
    }

    public void onClientData(final JsonObject event) {
        if (nativeHandlers != null && !nativeHandlers.isEmpty()) {
            final String nativeKey = ClientToServerModel.NATIVE.toStringValue();
            if (event.containsKey(nativeKey)) {
                final PNativeEvent nativeEvent = new PNativeEvent(this, event.getJsonObject(nativeKey));
                for (final PNativeHandler handler : nativeHandlers) {
                    handler.onNativeEvent(nativeEvent);
                }
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (ID ^ ID >>> 32);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final PObject other = (PObject) obj;
        if (ID != other.ID) return false;
        return true;
    }

    protected void saveAdd(final int objectID, final int parentObjectID) {
        saveAdd(objectID, parentObjectID, (ServerBinaryModel) null);
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final ServerBinaryModel... binaryModels) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) executeAdd(objectID, parentObjectID, binaryModels);
        else stackedInstructions.add(() -> executeAdd(objectID, parentObjectID, binaryModels));
    }

    protected void executeAdd(final int objectID, final int parentObjectID) {
        executeAdd(objectID, parentObjectID, (ServerBinaryModel) null);
    }

    protected void executeAdd(final int objectID, final int parentObjectID, final ServerBinaryModel... binaryModels) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (windowID != PWindow.MAIN_WINDOW_ID) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
        parser.parse(ServerToClientModel.TYPE_ADD, objectID);
        parser.parse(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        if (binaryModels != null) {
            for (final ServerBinaryModel binaryModel : binaryModels) {
                if (binaryModel != null) parser.parse(binaryModel.getKey(), binaryModel.getValue());
            }
        }
        parser.endObject();
        // UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveAddHandler(final HandlerModel type) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) executeAddHandler(type);
        else stackedInstructions.add(() -> executeAddHandler(type));
    }

    protected void executeAddHandler(final HandlerModel type) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) {
            final Parser parser = Txn.get().getParser();
            parser.beginObject();
            if (windowID != PWindow.MAIN_WINDOW_ID) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
            parser.parse(ServerToClientModel.TYPE_ADD_HANDLER, type.getValue());
            parser.parse(ServerToClientModel.OBJECT_ID, ID);
            parser.endObject();
        }
    }

    protected void saveRemoveHandler(final HandlerModel type) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) executeRemoveHandler();
        else stackedInstructions.add(() -> executeRemoveHandler());
    }

    protected void executeRemoveHandler() {
        if (windowID != PWindow.EMPTY_WINDOW_ID) {
            final Parser parser = Txn.get().getParser();
            parser.beginObject();
            if (windowID != PWindow.MAIN_WINDOW_ID) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
            parser.parse(ServerToClientModel.TYPE_REMOVE_HANDLER, ID);
            parser.endObject();
        }
    }

    protected void saveRemove(final int objectID, final int parentObjectID) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) executeRemove(objectID, parentObjectID);
        else stackedInstructions.add(() -> executeRemove(objectID, parentObjectID));
    }

    protected void executeRemove(final int objectID, final int parentObjectID) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) {
            final Parser parser = Txn.get().getParser();
            parser.beginObject();
            if (windowID != PWindow.MAIN_WINDOW_ID) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
            parser.parse(ServerToClientModel.TYPE_REMOVE, objectID);
            parser.parse(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
            parser.endObject();
        }
    }

    protected void saveUpdate(final ServerToClientModel model) {
        saveUpdate(model, null);
    }

    protected void saveUpdate(final ServerToClientModel model, final Object value) {
        saveUpdate(new ServerBinaryModel(model, value));
    }

    protected void saveUpdate(final ServerToClientModel model1, final Object value1, final ServerToClientModel model2,
            final Object value2) {
        saveUpdate(new ServerBinaryModel(model1, value1), new ServerBinaryModel(model2, value2));
    }

    protected void saveUpdate(final ServerBinaryModel... binaryModels) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) executeUpdate(binaryModels);
        else stackedInstructions.add(() -> executeUpdate(binaryModels));
    }

    protected void executeUpdate(final ServerToClientModel model, final Object value) {
        executeUpdate(new ServerBinaryModel(model, value));
    }

    private void executeUpdate(final ServerBinaryModel... binaryModels) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) {
            final Parser parser = Txn.get().getParser();
            parser.beginObject();
            if (windowID != PWindow.MAIN_WINDOW_ID) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
            parser.parse(ServerToClientModel.TYPE_UPDATE, ID);
            if (binaryModels != null) {
                for (final ServerBinaryModel binaryModel : binaryModels) {
                    if (binaryModel != null) parser.parse(binaryModel.getKey(), binaryModel.getValue());
                }
            }

            parser.endObject();
        }
    }

    @Override
    public String toString() {
        return "ID=" + ID + ", widgetType=" + getWidgetType().name();
    }

}
