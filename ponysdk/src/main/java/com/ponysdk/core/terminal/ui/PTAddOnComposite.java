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

package com.ponysdk.core.terminal.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.JavascriptAddOnFactory;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import elemental.json.JsonObject;

public class PTAddOnComposite extends PTAddOn {

    private static final Logger log = Logger.getLogger(PTAddOnComposite.class.getName());

    private final List<PAddOnExecution> pendingUpdates = new ArrayList<>();

    private Widget widget;

    private boolean initialized;

    @Override
    protected void doCreate(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        // ServerToClientModel.FACTORY
        final String signature = buffer.readBinaryModel().getStringValue();
        final JavascriptAddOnFactory factory = getFactory(uiBuilder, signature);

        final JsonObject arguments;
        BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.PADDON_CREATION == binaryModel.getModel()) {
            arguments = binaryModel.getJsonObject();
            binaryModel = buffer.readBinaryModel();
        } else {
            arguments = null;
        }

        final int widgetID = binaryModel.getIntValue();
        final PTWidget<?> object = (PTWidget<?>) uiBuilder.getPTObject(widgetID);
        widget = object.uiObject;

        widget.addAttachHandler(event -> {
            try {
                if (event.isAttached()) {
                    addOn.onAttached();
                    flushPendingUpdates();
                } else {
                    addOn.onDetached();
                }
            } catch (final JavaScriptException e) {
                log.log(Level.SEVERE, "PTAddOnComposite #" + getObjectID() + " (" + signature + ") " + e.getMessage(), e);
            }
        });

        try {
            addOn = factory.newAddOn(objectId, (JavaScriptObject) arguments, String.valueOf(widgetID), widget.getElement());
            addOn.onInit();
            if (widget.isAttached()) addOn.onAttached();
            initialized = true;
        } catch (final JavaScriptException e) {
            log.log(Level.SEVERE, "PTAddOnComposite #" + getObjectID() + " (" + signature + ") " + e.getMessage(), e);
        }
    }

    @Override
    protected void doUpdate(final String methodName, final JavaScriptObject arguments) {
        if (!destroyed) {
            if (initialized && widget.isAttached()) {
                flushPendingUpdates();
                super.doUpdate(methodName, arguments);
            } else {
                pendingUpdates.add(new PAddOnExecution(methodName, arguments));
            }
        } else {
            log.warning("PTAddOnComposite #" + getObjectID() + " destroyed, so updates will be discarded : " + methodName);
        }
    }

    private void flushPendingUpdates() {
        if (!pendingUpdates.isEmpty()) {
            for (final PAddOnExecution update : pendingUpdates) {
                super.doUpdate(update.getMethodName(), update.getArguments());
            }
            pendingUpdates.clear();
        }
    }

    @Override
    public void destroy() {
        if (!destroyed) {
            pendingUpdates.clear();
            super.destroy();
        }
    }

    private static final class PAddOnExecution {

        private final String methodName;
        private final JavaScriptObject arguments;

        public PAddOnExecution(final String methodName, final JavaScriptObject arguments) {
            this.methodName = methodName;
            this.arguments = arguments;
        }

        public String getMethodName() {
            return methodName;
        }

        public JavaScriptObject getArguments() {
            return arguments;
        }
    }
}
