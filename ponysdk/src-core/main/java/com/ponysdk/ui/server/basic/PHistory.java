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
package com.ponysdk.ui.server.basic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ponysdk.core.PonySession;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.instruction.History;

public class PHistory {

    private final List<PValueChangeHandler<String>> handlers = new CopyOnWriteArrayList<PValueChangeHandler<String>>();

    private String token;

    public void addValueChangeHandler(PValueChangeHandler<String> handler) {
        handlers.add(handler);
    }

    public void newItem(String token) {
        this.token = token;
        final History history = new History(token);
        PonySession.getCurrent().stackInstruction(history);
    }

    public void fireHistory() {

    }

    public void fireHistoryChanged(final String token) {
        this.token = token;
        for (final PValueChangeHandler<String> handler : handlers) {
            handler.onValueChange(token);
        }

    }

    public String getToken() {
        return token;
    }

}
