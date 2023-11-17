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

import com.google.gwt.user.client.Timer;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import elemental2.core.Global;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PTScript extends AbstractPTObject {

    private static final Logger log = Logger.getLogger(PTScript.class.getName());

    private static void eval(final String script, final long delayMillis) {
        if (delayMillis == -1) {
            try {
                Global.eval(script);
            } catch (final Throwable t) {
                log.log(Level.SEVERE, "PTScript exception for " + script, t);
            }
        } else {
            new Timer() {

                @Override
                public void run() {
                    try {
                        Global.eval(script);
                    } catch (final Throwable t) {
                        log.log(Level.SEVERE, "PTScript exception for " + script, t);
                    }
                }
            }.schedule((int) delayMillis);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.EVAL == model) {
            long commandID = -1;
            long delayMillis = -1;

            final String script = binaryModel.getStringValue();

            final BinaryModel commandIdModel = buffer.readBinaryModel();

            if (ServerToClientModel.COMMAND_ID == commandIdModel.getModel()) commandID = commandIdModel.getLongValue();
            else buffer.rewind(commandIdModel);

            final BinaryModel delayModel = buffer.readBinaryModel();
            if (ServerToClientModel.FIXDELAY == delayModel.getModel()) delayMillis = delayModel.getLongValue();
            else buffer.rewind(delayModel);

            if (commandID == -1) eval(script, delayMillis);
            else evalWithCallback(script, commandID, delayMillis);

            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    private void evalWithCallback(final String script, final long commandID, final long delayMillis) {
        if (delayMillis == -1) {
            try {
                sendResult(commandID, Global.eval(script));
            } catch (final Throwable t) {
                log.log(Level.SEVERE, "PTScript exception for " + script, t);
                sendError(commandID, t);
            }
        } else {
            new Timer() {

                @Override
                public void run() {
                    try {
                        sendResult(commandID, Global.eval(script));
                    } catch (final Throwable t) {
                        log.log(Level.SEVERE, "PTScript exception for " + script, t);
                        sendError(commandID, t);
                    }
                }
            }.schedule((int) delayMillis);
        }
    }

    private void sendResult(final long commandID, final Object result) {
        final PTInstruction eventInstruction = new PTInstruction(getObjectID());
        eventInstruction.put(ClientToServerModel.COMMAND_ID, commandID);
        eventInstruction.put(ClientToServerModel.RESULT, result == null ? "" : result.toString());
        uiBuilder.sendDataToServer(eventInstruction);
    }

    private void sendError(final long commandID, final Throwable t) {
        final PTInstruction eventInstruction = new PTInstruction(getObjectID());
        eventInstruction.put(ClientToServerModel.COMMAND_ID, commandID);
        eventInstruction.put(ClientToServerModel.ERROR_MSG, t.getMessage());
        uiBuilder.sendDataToServer(eventInstruction);
    }

}
