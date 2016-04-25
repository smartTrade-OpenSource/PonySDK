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

package com.ponysdk.ui.terminal.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public class PTScript extends AbstractPTObject {

    private final static Logger log = Logger.getLogger(PTScript.class.getName());

    private UIService uiService;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        super.create(buffer, objectId, uiService);
        this.uiService = uiService;
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.EVAL.equals(binaryModel.getModel())) {
            // ServerToClientModel.EVAL
            final String scriptToEval = binaryModel.getStringValue();
            final BinaryModel commandId = buffer.getBinaryModel();
            if (ServerToClientModel.COMMAND_ID.equals(commandId.getModel())) {
                try {
                    final Object result = evalWithCallback(scriptToEval);
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(getObjectID());
                    eventInstruction.put(ClientToServerModel.COMMAND_ID, commandId.getLongValue());
                    eventInstruction.put(ClientToServerModel.RESULT, result == null ? "" : result.toString());
                    uiService.sendDataToServer(eventInstruction);
                } catch (final Throwable e) {
                    log.log(Level.SEVERE, "PTScript exception for : " + scriptToEval, e);
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(getObjectID());
                    eventInstruction.put(ClientToServerModel.COMMAND_ID, commandId.getLongValue());
                    eventInstruction.put(ClientToServerModel.ERROR_MSG, e.getMessage());
                    uiService.sendDataToServer(eventInstruction);
                }
            } else {
                buffer.rewind(commandId);
                try {
                    eval(scriptToEval);
                } catch (final Throwable e) {
                    log.log(Level.SEVERE, "PTScript exception for : " + scriptToEval, e);
                }
            }
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    public static native void eval(String script) /*-{
                                                     $wnd.eval(script);
                                                     }-*/;

    public static native Object evalWithCallback(String script) /*-{
                                                                var r = $wnd.eval(script);
                                                                if (typeof r=="object") return JSON.stringify(r);
                                                                else return r;
                                                                }-*/;

}
