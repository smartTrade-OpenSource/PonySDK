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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.PTerminalEvent;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.util.SetUtils;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.core.writer.ModelWriterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Set;

import com.ponysdk.core.util.CompactIntRunnableMap;

/**
 * The superclass for all PonySDK objects.
 */
public abstract class PObject implements com.ponysdk.core.writer.UpdateTarget {

    private static final Logger log = LoggerFactory.getLogger(PObject.class);

    protected final int ID = UIContext.get().nextID();
    protected PWindow window;

    /**
     * Rarely used state, allocated on first use: every widget paid one reference slot per field
     * (native binding, terminal handler, ajax handler) although almost none use them.
     */
    private static final class Extras {
        private Set<InitializeListener> initializeListeners;
        private Set<DestroyListener> destroyListeners;
        private Object data;
        /** PLabel only: kept here so that every other widget does not pay for it. */
        private String attributeLinkedToValue;
        private String nativeBindingFunction;
        private PTerminalEvent.Handler terminalHandler;
        private AjaxHandler ajaxHandler;
        /** The frame the object lives in, when not directly in its window (rare: iframes). */
        private PFrame frame;
    }

    private Extras extras;

    /** Notifies the initialize listeners (end of the CREATE). */
    void fireInitializeListeners() {
        final Set<InitializeListener> listeners = extras != null ? extras.initializeListeners : null;
        if (listeners == null) return;
        extras.initializeListeners = null; // fired once: keeping them would retain what they capture
        listeners.forEach(listener -> listener.onInitialize(this));
    }

    String getAttributeLinkedToValue0() {
        return extras != null ? extras.attributeLinkedToValue : null;
    }

    void setAttributeLinkedToValue0(final String value) {
        if (value != null || extras != null) extras().attributeLinkedToValue = value;
    }

    private Extras extras() {
        if (extras == null) extras = new Extras();
        return extras;
    }

    protected CompactIntRunnableMap stackedInstructions;

    protected boolean initialized = false;
    protected boolean destroy = false;

    PObject() {
    }


    /**
     * Gets the widget type
     *
     * @return the widget type
     */
    protected abstract WidgetType getWidgetType();

    protected boolean attach(final PWindow window, final PFrame frame) {
        setFrame(frame);

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
        else if (getFrame() != null && !getFrame().isInitialized()) getFrame().addInitializeListener(event -> applyInit());
        else window.addOpenHandler(window -> applyInit());
    }

    public boolean isParentInitialized() {
        final PFrame frame = getFrame();
        return window.isOpened() && (frame == null || frame.isInitialized());
    }

    protected void applyInit() {
        final ModelWriter writer = writer();
        writer.beginObject(window);
        writeFrameId(writer);
        writer.writeInt(ServerToClientModel.TYPE_CREATE, ID);
        writer.write(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
        enrichForCreation(writer);
        enrichForUpdate(writer);
        writer.endCreate(ID); // protocol 3: an add written right after joins this block

        UIContext.get().registerObject(this);

        init0();

        if (stackedInstructions != null) {
            stackedInstructions.runAll();
            stackedInstructions = null;
        }

        initialized = true;

        fireInitializeListeners();
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
        return extras != null ? extras.frame : null;
    }

    protected void setFrame(final PFrame frame) {
        if (frame != null || extras != null) extras().frame = frame;
    }

    private void writeFrameId(final ModelWriter writer) {
        final PFrame frame = getFrame();
        if (frame != null) writer.writeInt(ServerToClientModel.FRAME_ID, frame.getID());
    }

    public final int getID() {
        return ID;
    }

    /**
     * Bind to a Terminal function, useful to link the objectID and the widget
     * reference
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
     */
    public void bindTerminalFunction(final String functionName) {
        if (extras != null && extras.nativeBindingFunction != null)
            throw new IllegalAccessError("Object already bind to native function: " + extras.nativeBindingFunction);

        extras().nativeBindingFunction = functionName;

        saveUpdate(writer -> writer.write(ServerToClientModel.BIND, functionName));
    }

    /**
     * Calls {@code window[functionName](element, args...)} in the terminal, the element being this object's: a
     * function the application's own scripts define (ADR-0015).
     *
     * @param args strings, numbers, booleans or null
     */
    public void callTerminalFunction(final String functionName, final Object... args) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.FUNCTION_NAME, functionName);
            if (args != null && args.length > 0) writer.write(ServerToClientModel.FUNCTION_ARGS, args);
        });
    }

    /** @deprecated JavaScript source needs {@code 'unsafe-eval'}: {@link #callTerminalFunction(String, Object...)} */
    @Deprecated(since = "3.0")
    public void evalTerminalScript(final String script) {
        requireScriptEval("evalTerminalScript", "callTerminalFunction");
        saveUpdate(writer -> writer.write(ServerToClientModel.EVAL, script));
    }

    /**
     * Refuses an API that sends JavaScript source when the application runs without eval
     * ({@link com.ponysdk.core.server.application.ApplicationConfiguration#setScriptEvalEnabled(boolean)}): the browser
     * would block it, here the developer learns which call to replace.
     */
    static void requireScriptEval(final String api, final String replacement) {
        final UIContext context = UIContext.get();
        if (context != null && !context.getConfiguration().isScriptEvalEnabled())
            throw new IllegalStateException(api + " sends JavaScript source, and this application runs without eval "
                    + "(ApplicationConfiguration.setScriptEvalEnabled(false)): call a function of the page by name with "
                    + replacement);
    }

    public void sendToNative(final JsonObject data) {
        if (destroy) return;
        if (extras == null || extras.nativeBindingFunction == null) throw new IllegalAccessError("Object not bind to a native function");

        saveUpdate(writer -> writer.write(ServerToClientModel.NATIVE, data.toString()));
    }

    public void setTerminalHandler(final PTerminalEvent.Handler terminalHandler) {
        if (terminalHandler != null || extras != null) extras().terminalHandler = terminalHandler;
    }

    /**
     * JSON received from the Terminal using pony.sendDataToServer(objectID, JSON)
     *
     * @param event Json received from the terminal
     */
    public void onClientData(final JsonObject event) {
        if (destroy) return;
        final PTerminalEvent.Handler terminalHandler = extras != null ? extras.terminalHandler : null;
        if (terminalHandler != null) {
            final String nativeKey = ClientToServerModel.NATIVE.toStringValue();
            if (event.containsKey(nativeKey)) {
                terminalHandler.onTerminalEvent(new PTerminalEvent(this, event.getJsonObject(nativeKey)));
            }
        }
    }

    protected CompactIntRunnableMap safeStackedInstructions() {
        if (stackedInstructions == null) stackedInstructions = new CompactIntRunnableMap(2); // usually the add + one
        return stackedInstructions;
    }

    protected void saveUpdate(final ModelWriterCallback callback) {
        if (destroy) return;
        if (initialized) writeUpdate(callback);
        else safeStackedInstructions().append(() -> writeUpdate(callback));
    }

    protected void saveUpdate(final ServerToClientModel serverToClientModel, final Object value) {
        if (destroy) return;
        // Hot path (every setter of a live widget): coalesced when the model allows it, otherwise
        // written directly - no capturing lambda per update either way
        if (initialized) {
            if (!writer().deferUpdate(this, serverToClientModel, value)) writeUpdate(serverToClientModel, value);
        }
        else safeStackedInstructions().put(serverToClientModel.getValue(), () -> writeUpdate(serverToClientModel, value));
    }

    /** Keyed update without value (a class added / removed, a style or attribute removed): coalesced per name. */
    protected void saveKeyedUpdate(final ServerToClientModel keyModel, final String key) {
        if (destroy) return;
        if (initialized) {
            if (!writer().deferUpdate(this, keyModel, key)) writeUpdate(keyModel, key);
        }
        else safeStackedInstructions().append(() -> writeUpdate(keyModel, key));
    }

    /** Two-field update ({@code key1=value1, key2=value2}), e.g. a style or attribute name + value. */
    protected void saveUpdate(final ServerToClientModel key1, final Object value1,
                              final ServerToClientModel key2, final Object value2) {
        if (destroy) return;
        if (initialized) {
            if (!(value1 instanceof String key && writer().deferUpdate(this, key1, key, key2, value2))) {
                writeUpdate(key1, value1, key2, value2);
            }
        }
        else safeStackedInstructions().append(() -> writeUpdate(key1, value1, key2, value2));
    }

    /** The writer of this object's context, resolved through its window when possible (no ThreadLocal). */
    private ModelWriter writer() {
        final PWindow w = window;
        final ModelWriter writer = w != null ? w.contextWriter() : null;
        return writer != null ? writer : UIContext.get().getWriter();
    }

    /** {@link com.ponysdk.core.writer.UpdateTarget}: updates of one run share the window / frame header. */
    @Override
    public Object updateGroup() {
        final PFrame frame = getFrame();
        return frame != null ? frame : window;
    }

    @Override
    public boolean isUpdatable() {
        return initialized && !destroy;
    }

    /** {@link com.ponysdk.core.writer.UpdateTarget}: opens the update block of a coalesced update. */
    @Override
    public boolean beginCoalescedUpdate(final ModelWriter writer) {
        if (destroy) return false;
        writer.beginObject(window);
        writeFrameId(writer);
        writer.writeInt(ServerToClientModel.TYPE_UPDATE, ID);
        return true;
    }

    private ModelWriter beginUpdate() {
        final ModelWriter writer = writer();
        writer.beginObject(window);
        writeFrameId(writer);
        writer.writeInt(ServerToClientModel.TYPE_UPDATE, ID);
        return writer;
    }

    private void writeUpdate(final ServerToClientModel model, final Object value) {
        if (destroy) return;
        final ModelWriter writer = beginUpdate();
        writer.write(model, value);
        writer.endObject();
    }

    private void writeUpdate(final ServerToClientModel key1, final Object value1,
                             final ServerToClientModel key2, final Object value2) {
        if (destroy) return;
        final ModelWriter writer = beginUpdate();
        writer.write(key1, value1);
        writer.write(key2, value2);
        writer.endObject();
    }

    void writeUpdate(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = beginUpdate();
        callback.doWrite(writer);
        writer.endObject();
    }

    protected void saveAdd(final int objectID, final int parentObjectID) {
        if (destroy) return;
        // The common add (no extra model): one capture when deferred, none when written right away
        if (initialized) writeAdd(objectID, parentObjectID);
        else safeStackedInstructions().append(() -> writeAdd(objectID, parentObjectID));
    }

    private void writeAdd(final int objectID, final int parentObjectID) {
        if (destroy) return;
        final ModelWriter writer = writer();
        if (writer.addToOpenCreate(objectID, parentObjectID)) return; // created in its parent
        writer.beginObject(window);
        writeFrameId(writer);
        writer.writeInt(ServerToClientModel.TYPE_ADD, objectID);
        writer.writeInt(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        writer.endObject();
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final ServerBinaryModel... binaryModels) {
        if (destroy) return;

        final ModelWriterCallback callback = writer -> {
            writer.writeInt(ServerToClientModel.TYPE_ADD, objectID);
            writer.writeInt(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
            if (binaryModels != null) {
                for (final ServerBinaryModel binaryModel : binaryModels) {
                    if (binaryModel != null) writer.write(binaryModel.getKey(), binaryModel.getValue());
                }
            }
        };
        if (initialized) writeAdd(callback);
        else safeStackedInstructions().append(() -> writeAdd(callback));
    }

    private void writeAdd(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = writer();
        writer.beginObject(window);
        writeFrameId(writer);

        callback.doWrite(writer);
        writer.endObject();
    }

    protected void saveAddHandler(final HandlerModel type) {
        if (destroy) return;

        final ModelWriterCallback callback = writer -> writer.write(ServerToClientModel.HANDLER_TYPE, type.getValue());
        if (initialized) writeAddHandler(callback);
        else safeStackedInstructions().append(() -> writeAddHandler(callback));
    }

    void writeAddHandler(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = writer();
        writer.beginObject(window);
        writeFrameId(writer);
        writer.writeInt(ServerToClientModel.TYPE_ADD_HANDLER, ID);

        callback.doWrite(writer);
        writer.endObject();
    }

    protected void saveRemoveHandler(final HandlerModel type) {
        if (destroy) return;

        final ModelWriterCallback callback = writer -> {
            writer.write(ServerToClientModel.TYPE_REMOVE_HANDLER);
            // protocol 4: the object, which the NULL model above cannot carry (ADR-0003)
            if (UIContext.get().getProtocolVersion() >= ProtocolVersion.LISTENERS) writer.writeInt(ServerToClientModel.WIDGET_ID, ID);
            writer.write(ServerToClientModel.HANDLER_TYPE, type.getValue());
        };
        if (initialized) writeRemoveHandler(callback);
        else safeStackedInstructions().append(() -> writeRemoveHandler(callback));
    }

    private void writeRemoveHandler(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = writer();
        writer.beginObject(window);
        writeFrameId(writer);

        callback.doWrite(writer);
        writer.endObject();
    }

    void saveRemove(final int objectID, final int parentObjectID) {
        if (destroy) return;

        final ModelWriterCallback callback = writer -> {
            writer.writeInt(ServerToClientModel.TYPE_REMOVE, objectID);
            writer.writeInt(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        };
        if (initialized) writeRemove(callback);
        else safeStackedInstructions().append(() -> writeRemove(callback));
    }

    private void writeRemove(final ModelWriterCallback callback) {
        if (destroy) return;

        final ModelWriter writer = writer();
        writer.beginObject(window);
        writeFrameId(writer);

        callback.doWrite(writer);
        writer.endObject();
    }

    public void addInitializeListener(final InitializeListener listener) {
        if (initialized) return; // already fired, it would never be called
        final Extras x = extras();
        if (x.initializeListeners == null) x.initializeListeners = SetUtils.newArraySet(4);
        x.initializeListeners.add(listener);
    }

    public void addDestroyListener(final DestroyListener listener) {
        final Extras x = extras();
        if (x.destroyListeners == null) x.destroyListeners = SetUtils.newArraySet(4);
        x.destroyListeners.add(listener);
    }

    public void removeDestroyListener(final DestroyListener listener) {
        if (extras != null && extras.destroyListeners != null) extras.destroyListeners.remove(listener);
    }

    public void onDestroy() {
        destroy = true;
        if (extras != null) extras.terminalHandler = null;
        final Set<DestroyListener> destroyListeners = extras != null ? extras.destroyListeners : null;
        if (extras != null) {
            extras.initializeListeners = null;
            extras.destroyListeners = null;
        }
        if (destroyListeners != null) {
            destroyListeners.forEach(this::doDestroy);
        }
        window = null;
    }

    private void doDestroy(DestroyListener listener) {
        try {
            listener.onDestroy(this);
        } catch (Exception e) {
            log.error("An error occurred while trying to process the destroy event", e);
        }
    }

    public void setData(final Object data) {
        if (data != null || extras != null) extras().data = data;
    }

    public Object getData() {
        return extras != null ? extras.data : null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
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
        if (httpRequestHandler != null || extras != null) extras().ajaxHandler = httpRequestHandler;
    }

    public final void handleAjaxRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final AjaxHandler ajaxHandler = extras != null ? extras.ajaxHandler : null;
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
