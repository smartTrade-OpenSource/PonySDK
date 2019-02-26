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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.JavascriptAddOn;
import com.ponysdk.core.terminal.JavascriptAddOnFactory;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import elemental.json.JsonObject;

public class PTAddOn extends AbstractPTObject {

    private static final Logger log = Logger.getLogger(PTAddOn.class.getName());

    protected boolean destroyed;

    JavascriptAddOn addOn;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);
        doCreate(buffer, objectId, uiBuilder);
    }

    protected void doCreate(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        // ServerToClientModel.FACTORY
        final String signature = buffer.readBinaryModel().getStringValue();
        final JavascriptAddOnFactory factory = getFactory(uiBuilder, signature);

        final JsonObject arguments;
        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.PADDON_CREATION == binaryModel.getModel()) {
            arguments = binaryModel.getJsonObject();
        } else {
            arguments = null;
            buffer.rewind(binaryModel);
        }

        try {
            addOn = factory.newAddOn(objectId, (JavaScriptObject) arguments, null, null);
            addOn.onInit();
        } catch (final JavaScriptException e) {
            log.log(Level.SEVERE, "PTAddOn #" + getObjectID() + " (" + signature + ") " + e.getMessage(), e);
        }
    }

    protected static final JavascriptAddOnFactory getFactory(final UIBuilder uiBuilder, final String signature) {
        final JavascriptAddOnFactory factory = uiBuilder.getJavascriptAddOnFactory(signature);
        if (factory != null) return factory;
        else throw new IllegalArgumentException("AddOn factory not found for signature: " + signature);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.PADDON_METHOD == model) {
            final String methodName = binaryModel.getStringValue();
            final BinaryModel arguments = buffer.readBinaryModel();
            if (ServerToClientModel.PADDON_ARGUMENTS == arguments.getModel()) {
                doUpdate(methodName, arguments.getArrayValue().getJavaScriptObject());
            } else {
                buffer.rewind(arguments);
                doUpdate(methodName, null);
            }
            return true;
        } else if (ServerToClientModel.DESTROY == model) {
            destroy();
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    protected void doUpdate(final String methodName, final JavaScriptObject arguments) {
        try {
            if (!destroyed) addOn.update(methodName, arguments);
            else log.warning("PTAddOn #" + getObjectID() + " destroyed, so updates will be discarded : " + arguments);
        } catch (final JavaScriptException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        if (!destroyed) {
            addOn.destroy();
            destroyed = true;
        }
    }
}
