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

package com.ponysdk.core.terminal.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.json.client.JSONArray;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

/**
 * Terminal-side handler for PComponent.
 * <p>
 * This class bridges the GWT terminal with the TypeScript ComponentTerminal.
 * It receives binary messages from the server and forwards them to the
 * TypeScript terminal for actual component rendering.
 * </p>
 * <p>
 * The PTComponent operates alongside existing PAddOn components without
 * conflicts, allowing progressive migration from PAddOn to PComponent
 * (Requirements 13.1, 13.2).
 * </p>
 *
 * @see com.ponysdk.core.ui.component.PComponent
 */
public class PTComponent extends AbstractPTObject {

    private static final Logger log = Logger.getLogger(PTComponent.class.getName());

    /**
     * The framework type (React, Vue, Svelte, WebComponent).
     */
    private byte frameworkType;

    /**
     * The component signature for factory lookup.
     */
    private String signature;

    /**
     * The initial props JSON string.
     */
    private String initialProps;

    /**
     * Flag indicating whether the component has been destroyed.
     */
    private boolean destroyed = false;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);
        doCreate(buffer, objectId);
    }

    /**
     * Processes the component creation message.
     * <p>
     * Reads the framework type, signature, and initial props from the buffer,
     * then forwards the creation to the TypeScript ComponentTerminal.
     * </p>
     */
    private void doCreate(final ReaderBuffer buffer, final int objectId) {
        // Read PCOMPONENT_CREATE marker
        BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.PCOMPONENT_CREATE != binaryModel.getModel()) {
            log.warning("PTComponent #" + objectId + " expected PCOMPONENT_CREATE, got: " + binaryModel.getModel());
            buffer.rewind(binaryModel);
            return;
        }

        // Read PCOMPONENT_FRAMEWORK
        binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.PCOMPONENT_FRAMEWORK == binaryModel.getModel()) {
            frameworkType = (byte) binaryModel.getIntValue();
        } else {
            log.warning("PTComponent #" + objectId + " expected PCOMPONENT_FRAMEWORK, got: " + binaryModel.getModel());
            buffer.rewind(binaryModel);
            return;
        }

        // Read PCOMPONENT_SIGNATURE
        binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.PCOMPONENT_SIGNATURE == binaryModel.getModel()) {
            signature = binaryModel.getStringValue();
        } else {
            log.warning("PTComponent #" + objectId + " expected PCOMPONENT_SIGNATURE, got: " + binaryModel.getModel());
            buffer.rewind(binaryModel);
            return;
        }

        // Read PCOMPONENT_PROPS_FULL
        binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.PCOMPONENT_PROPS_FULL == binaryModel.getModel()) {
            initialProps = binaryModel.getStringValue();
        } else {
            log.warning("PTComponent #" + objectId + " expected PCOMPONENT_PROPS_FULL, got: " + binaryModel.getModel());
            buffer.rewind(binaryModel);
            return;
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("PTComponent #" + objectId + " created: framework=" + frameworkType 
                    + ", signature=" + signature + ", props=" + initialProps);
        }

        // Forward to TypeScript ComponentTerminal
        notifyComponentCreate(objectId, frameworkType, signature, initialProps);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();

        if (ServerToClientModel.PCOMPONENT_UPDATE == model) {
            // Read the props update type
            final BinaryModel propsModel = buffer.readBinaryModel();
            final ServerToClientModel propsType = propsModel.getModel();

            if (ServerToClientModel.PCOMPONENT_PROPS_PATCH == propsType) {
                // JSON Patch update
                final String patch = propsModel.getStringValue();
                if (log.isLoggable(Level.FINE)) {
                    log.fine("PTComponent #" + getObjectID() + " received patch: " + patch);
                }
                notifyComponentPatch(getObjectID(), patch);
                return true;
            } else if (ServerToClientModel.PCOMPONENT_PROPS_FULL == propsType) {
                // Full props update
                final String props = propsModel.getStringValue();
                if (log.isLoggable(Level.FINE)) {
                    log.fine("PTComponent #" + getObjectID() + " received full props: " + props);
                }
                notifyComponentProps(getObjectID(), props);
                return true;
            } else if (ServerToClientModel.PCOMPONENT_PROPS_BINARY == propsType) {
                // Binary update - data comes as JSONArray of byte values
                final JSONArray binaryArray = propsModel.getArrayValue();
                if (log.isLoggable(Level.FINE)) {
                    log.fine("PTComponent #" + getObjectID() + " received binary update: " + binaryArray.size() + " bytes");
                }
                notifyComponentBinary(getObjectID(), binaryArray);
                return true;
            } else {
                log.warning("PTComponent #" + getObjectID() + " unexpected props type: " + propsType);
                buffer.rewind(propsModel);
                return false;
            }
        } else if (ServerToClientModel.DESTROY == model) {
            destroy();
            return true;
        }

        return super.update(buffer, binaryModel);
    }

    @Override
    public void destroy() {
        if (!destroyed) {
            destroyed = true;
            if (log.isLoggable(Level.FINE)) {
                log.fine("PTComponent #" + getObjectID() + " destroyed");
            }
            notifyComponentDestroy(getObjectID());
        }
    }

    /**
     * Returns the framework type for this component.
     *
     * @return the framework type byte value
     */
    public byte getFrameworkType() {
        return frameworkType;
    }

    /**
     * Returns the component signature.
     *
     * @return the signature string
     */
    public String getSignature() {
        return signature;
    }

    // ========================================================================
    // Native JavaScript methods for TypeScript ComponentTerminal integration
    // ========================================================================

    /**
     * Notifies the TypeScript ComponentTerminal of a new component creation.
     * <p>
     * The TypeScript terminal will create the appropriate framework adapter
     * and mount the component.
     * </p>
     */
    private native void notifyComponentCreate(int objectId, byte framework, String signature, String props) /*-{
        if ($wnd.PonySDK && $wnd.PonySDK.ComponentTerminal) {
            $wnd.PonySDK.ComponentTerminal.handleCreate(objectId, framework, signature, props);
        } else {
            // ComponentTerminal not loaded - this is expected if using GWT-only mode
            // Log at fine level since this is not an error condition
            if ($wnd.console && $wnd.console.debug) {
                $wnd.console.debug('PonySDK ComponentTerminal not available for component #' + objectId);
            }
        }
    }-*/;

    /**
     * Notifies the TypeScript ComponentTerminal of a JSON Patch update.
     */
    private native void notifyComponentPatch(int objectId, String patch) /*-{
        if ($wnd.PonySDK && $wnd.PonySDK.ComponentTerminal) {
            $wnd.PonySDK.ComponentTerminal.handlePatch(objectId, patch);
        }
    }-*/;

    /**
     * Notifies the TypeScript ComponentTerminal of a full props update.
     */
    private native void notifyComponentProps(int objectId, String props) /*-{
        if ($wnd.PonySDK && $wnd.PonySDK.ComponentTerminal) {
            $wnd.PonySDK.ComponentTerminal.handleProps(objectId, props);
        }
    }-*/;

    /**
     * Notifies the TypeScript ComponentTerminal of a binary update.
     * The binary data is passed as a JSONArray of byte values.
     */
    private native void notifyComponentBinary(int objectId, JSONArray binaryArray) /*-{
        if ($wnd.PonySDK && $wnd.PonySDK.ComponentTerminal) {
            // Convert JSONArray to JavaScript Uint8Array
            var length = binaryArray.size();
            var uint8Array = new Uint8Array(length);
            for (var i = 0; i < length; i++) {
                var value = binaryArray.get(i);
                uint8Array[i] = value.isNumber() ? value.isNumber().doubleValue() & 0xFF : 0;
            }
            $wnd.PonySDK.ComponentTerminal.handleBinary(objectId, uint8Array.buffer);
        }
    }-*/;

    /**
     * Notifies the TypeScript ComponentTerminal of component destruction.
     */
    private native void notifyComponentDestroy(int objectId) /*-{
        if ($wnd.PonySDK && $wnd.PonySDK.ComponentTerminal) {
            $wnd.PonySDK.ComponentTerminal.handleDestroy(objectId);
        }
    }-*/;
}
